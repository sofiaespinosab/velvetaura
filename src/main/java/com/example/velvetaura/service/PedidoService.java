package com.example.velvetaura.service;

import com.example.velvetaura.model.CambioEstado;
import com.example.velvetaura.model.Cliente;
import com.example.velvetaura.model.DetallePedido;
import com.example.velvetaura.model.EstadoPedido;
import com.example.velvetaura.model.Pedido;
import com.example.velvetaura.model.Prioridad;
import com.example.velvetaura.model.Producto;
import com.example.velvetaura.model.TipoEvento;
import com.example.velvetaura.repository.PedidoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

@Service
public class PedidoService {


    public record ItemPedido(String sku, int cantidad) {
    }


    private static final Comparator<Pedido> ORDEN_ATENCION = Comparator
            .comparing(Pedido::getPrioridad)
            .thenComparing(Pedido::getFechaCreacion)
            .thenComparing(Pedido::getId, Comparator.nullsLast(Comparator.<Long>naturalOrder()));

    private final PedidoRepository repository;
    private final ProductoService productoService;
    private final ClienteService clienteService;
    private final EventoService eventoService;


    private final Map<Long, Pedido> pedidos = new HashMap<>();


    private final PriorityQueue<Pedido> cola = new PriorityQueue<>(ORDEN_ATENCION);


    private final Stack<CambioEstado> cambios = new Stack<>();

    public PedidoService(PedidoRepository repository,
                         ProductoService productoService,
                         ClienteService clienteService,
                         EventoService eventoService) {
        this.repository = repository;
        this.productoService = productoService;
        this.clienteService = clienteService;
        this.eventoService = eventoService;
    }


    @PostConstruct
    public void cargarPedidos() {
        for (Pedido p : repository.findAll()) {
            pedidos.put(p.getId(), p);
            if (p.getEstado() == EstadoPedido.PENDIENTE) {
                cola.add(p);
            }
        }
    }


    public Pedido crearPedido(String identificacionCliente, Prioridad prioridad, List<ItemPedido> items) {
        Cliente cliente = clienteService.buscarPorIdentificacion(identificacionCliente)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un cliente registrado"));
        if (prioridad == null) {
            throw new IllegalArgumentException("Elige la prioridad del pedido");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un producto al pedido");
        }


        Map<String, Integer> cantidadPorSku = new LinkedHashMap<>();
        for (ItemPedido item : items) {
            if (item.cantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor que 0");
            }
            cantidadPorSku.merge(item.sku().trim().toUpperCase(), item.cantidad(), Integer::sum);
        }


        Map<String, Producto> productos = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : cantidadPorSku.entrySet()) {
            String sku = entrada.getKey();
            Producto producto = productoService.buscarPorSku(sku)
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + sku));
            if (!productoService.hayStock(sku, entrada.getValue())) {
                throw new IllegalArgumentException("Stock insuficiente para " + producto.getNombre()
                        + " (disponible: " + producto.getStock() + ")");
            }
            productos.put(sku, producto);
        }


        Pedido pedido = new Pedido(cliente, prioridad);
        for (Map.Entry<String, Integer> entrada : cantidadPorSku.entrySet()) {
            Producto producto = productos.get(entrada.getKey());
            pedido.agregarDetalle(new DetallePedido(
                    producto.getSku(), producto.getNombre(), entrada.getValue(), producto.getPrecio()));
        }


        for (Map.Entry<String, Integer> entrada : cantidadPorSku.entrySet()) {
            productoService.descontarStock(entrada.getKey(), entrada.getValue());
        }
        Pedido guardado = repository.save(pedido);
        pedidos.put(guardado.getId(), guardado);
        cola.add(guardado);

        eventoService.registrar(TipoEvento.PEDIDO_CREADO,
                "Pedido #" + guardado.getId() + " creado para " + cliente.getNombre()
                        + " (" + prioridad + "), total: " + guardado.getTotal());
        return guardado;
    }


    public List<Pedido> verColaOrdenada() {
        PriorityQueue<Pedido> copia = new PriorityQueue<>(cola);
        List<Pedido> ordenados = new ArrayList<>();
        while (!copia.isEmpty()) {
            ordenados.add(copia.poll());
        }
        return ordenados;
    }


    public Pedido atenderSiguiente() {
        Pedido siguiente = cola.poll();
        if (siguiente == null) {
            throw new IllegalStateException("No hay pedidos pendientes");
        }
        cambiarEstado(siguiente, EstadoPedido.EN_PREPARACION);
        return siguiente;
    }


    public Pedido avanzarEstado(Long id) {
        Pedido pedido = pedidos.get(id);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido no encontrado");
        }
        EstadoPedido anterior = pedido.getEstado();
        EstadoPedido nuevo;
        if (anterior == EstadoPedido.EN_PREPARACION) {
            nuevo = EstadoPedido.ENVIADO;
        } else if (anterior == EstadoPedido.ENVIADO) {
            nuevo = EstadoPedido.ENTREGADO;
        } else {
            throw new IllegalArgumentException("Solo se puede avanzar un pedido EN_PREPARACION o ENVIADO "
                    + "(estado actual: " + anterior + ")");
        }
        cambios.push(new CambioEstado(id, anterior, nuevo));
        cambiarEstado(pedido, nuevo);
        return pedido;
    }


    public Pedido deshacerUltimoCambio() {
        if (cambios.isEmpty()) {
            throw new IllegalStateException("No hay cambios por deshacer");
        }
        CambioEstado ultimo = cambios.pop();
        Pedido pedido = pedidos.get(ultimo.pedidoId());
        cambiarEstado(pedido, ultimo.estadoAnterior());
        return pedido;
    }


    public List<Pedido> listarPedidos() {
        List<Pedido> lista = new ArrayList<>(pedidos.values());
        lista.sort(Comparator.comparing(Pedido::getId).reversed());
        return lista;
    }

    private void cambiarEstado(Pedido pedido, EstadoPedido nuevo) {
        EstadoPedido anterior = pedido.getEstado();
        pedido.setEstado(nuevo);
        repository.save(pedido);
        eventoService.registrar(TipoEvento.ESTADO_CAMBIADO,
                "Pedido #" + pedido.getId() + ": " + anterior + " -> " + nuevo);
    }
}
