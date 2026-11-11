package com.example.velvetaura.service;

import com.example.velvetaura.model.Categoria;
import com.example.velvetaura.model.Cliente;
import com.example.velvetaura.model.EstadoPedido;
import com.example.velvetaura.model.Pedido;
import com.example.velvetaura.model.Prioridad;
import com.example.velvetaura.model.Producto;
import com.example.velvetaura.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedidoServiceTest {

    private PedidoRepository repository;
    private ProductoService productoService;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        repository = mock(PedidoRepository.class);
        productoService = mock(ProductoService.class);
        ClienteService clienteService = mock(ClienteService.class);
        EventoService eventoService = mock(EventoService.class);


        AtomicLong ids = new AtomicLong();
        when(repository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            if (pedido.getId() == null) {
                pedido.setId(ids.incrementAndGet());
            }
            return pedido;
        });

        Cliente cliente = new Cliente();
        cliente.setIdentificacion("123");
        cliente.setNombre("Ana");
        cliente.setCorreo("ana@correo.com");
        when(clienteService.buscarPorIdentificacion("123")).thenReturn(Optional.of(cliente));

        Producto labial = new Producto();
        labial.setSku("LAB01");
        labial.setNombre("Labial rojo");
        labial.setCategoria(Categoria.MAQUILLAJE);
        labial.setPrecio(25000);
        labial.setStock(10);
        when(productoService.buscarPorSku("LAB01")).thenReturn(Optional.of(labial));
        when(productoService.hayStock("LAB01", 2)).thenReturn(true);
        when(productoService.hayStock("LAB01", 50)).thenReturn(false);

        service = new PedidoService(repository, productoService, clienteService, eventoService);
    }

    private Pedido crear(Prioridad prioridad) {
        return service.crearPedido("123", prioridad, List.of(new PedidoService.ItemPedido("LAB01", 2)));
    }

    @Test
    void crearPedido_queda_pendiente_con_total_y_descuenta_stock() {
        Pedido pedido = crear(Prioridad.NORMAL);

        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertEquals(50000.0, pedido.getTotal());
        verify(productoService).descontarStock("LAB01", 2);
    }

    @Test
    void crearPedido_sin_stock_suficiente_no_crea_pedido_ni_descuenta() {
        assertThrows(IllegalArgumentException.class, () ->
                service.crearPedido("123", Prioridad.NORMAL,
                        List.of(new PedidoService.ItemPedido("LAB01", 50))));

        verify(productoService, never()).descontarStock(anyString(), anyInt());
        verify(repository, never()).save(any(Pedido.class));
    }

    @Test
    void cola_atiende_primero_express_y_luego_por_orden_de_llegada() {
        crear(Prioridad.NORMAL);
        crear(Prioridad.NORMAL);
        crear(Prioridad.EXPRESS);

        List<Pedido> cola = service.verColaOrdenada();

        assertEquals(3L, cola.get(0).getId().longValue());
        assertEquals(1L, cola.get(1).getId().longValue());
        assertEquals(2L, cola.get(2).getId().longValue());
    }

    @Test
    void deshacer_restaura_el_estado_anterior() {
        Pedido pedido = crear(Prioridad.NORMAL);
        service.atenderSiguiente();
        service.avanzarEstado(pedido.getId());
        assertEquals(EstadoPedido.ENVIADO, pedido.getEstado());

        service.deshacerUltimoCambio();

        assertEquals(EstadoPedido.EN_PREPARACION, pedido.getEstado());
    }

    @Test
    void deshacer_sin_cambios_lanza_error() {
        assertThrows(IllegalStateException.class, () -> service.deshacerUltimoCambio());
    }

    @Test
    void no_se_puede_avanzar_un_pedido_pendiente() {
        Pedido pedido = crear(Prioridad.NORMAL);

        assertThrows(IllegalArgumentException.class, () -> service.avanzarEstado(pedido.getId()));
    }
}
