package com.example.velvetaura.service;

import com.example.velvetaura.model.Categoria;
import com.example.velvetaura.model.Producto;
import com.example.velvetaura.model.TipoEvento;
import com.example.velvetaura.repository.ProductoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final EventoService eventoService;

    // Tabla hash: la clave es el SKU, el valor es el producto (consulta en O(1))
    private final Map<String, Producto> catalogo = new HashMap<>();

    public ProductoService(ProductoRepository repository, EventoService eventoService) {
        this.repository = repository;
        this.eventoService = eventoService;
    }

    // Al iniciar la aplicación se llena el HashMap con lo que hay en PostgreSQL
    @PostConstruct
    public void cargarCatalogo() {
        for (Producto p : repository.findAll()) {
            catalogo.put(p.getSku(), p);
        }
    }

    private String normalizar(String sku) {
        return sku == null ? "" : sku.trim().toUpperCase();
    }

    // F01
    public Producto registrar(Producto producto) {
        String sku = normalizar(producto.getSku());
        if (catalogo.containsKey(sku)) {
            throw new IllegalArgumentException("Ya existe un producto con el SKU " + sku);
        }
        producto.setSku(sku);
        Producto guardado = repository.save(producto);
        catalogo.put(sku, guardado);
        return guardado;
    }

    // F02
    public Optional<Producto> buscarPorSku(String sku) {
        return Optional.ofNullable(catalogo.get(normalizar(sku)));
    }

    // F03: si categoria es null devuelve todos
    public List<Producto> listar(Categoria categoria) {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : catalogo.values()) {
            if (categoria == null || p.getCategoria() == categoria) {
                resultado.add(p);
            }
        }
        resultado.sort(Comparator.comparing(Producto::getNombre));
        return resultado;
    }

    // F04
    public void reponerStock(String sku, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }
        Producto producto = catalogo.get(normalizar(sku));
        if (producto == null) {
            throw new IllegalArgumentException("Producto no encontrado");
        }
        producto.setStock(producto.getStock() + cantidad);
        repository.save(producto);
        eventoService.registrar(TipoEvento.STOCK_ACTUALIZADO,
                "Se sumaron " + cantidad + " unidades al producto " + producto.getSku()
                        + " (stock actual: " + producto.getStock() + ")");
    }

    // Para los pedidos (F06): ¿hay suficiente stock de este producto?
    public boolean hayStock(String sku, int cantidad) {
        Producto producto = catalogo.get(normalizar(sku));
        return producto != null && cantidad > 0 && producto.getStock() >= cantidad;
    }

    // Para los pedidos (F06): resta unidades. Primero se debe comprobar con hayStock
    public void descontarStock(String sku, int cantidad) {
        Producto producto = catalogo.get(normalizar(sku));
        if (producto == null || cantidad <= 0 || producto.getStock() < cantidad) {
            throw new IllegalArgumentException("Stock insuficiente para el producto " + sku);
        }
        producto.setStock(producto.getStock() - cantidad);
        repository.save(producto);
    }
}
