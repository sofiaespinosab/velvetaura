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


    private final Map<String, Producto> catalogo = new HashMap<>();

    public ProductoService(ProductoRepository repository, EventoService eventoService) {
        this.repository = repository;
        this.eventoService = eventoService;
    }


    @PostConstruct
    public void cargarCatalogo() {
        for (Producto p : repository.findAll()) {
            catalogo.put(p.getSku(), p);
        }
    }

    private String normalizar(String sku) {
        return sku == null ? "" : sku.trim().toUpperCase();
    }


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


    public Optional<Producto> buscarPorSku(String sku) {
        return Optional.ofNullable(catalogo.get(normalizar(sku)));
    }


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
}
