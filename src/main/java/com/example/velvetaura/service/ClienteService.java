package com.example.velvetaura.service;

import com.example.velvetaura.model.Cliente;
import com.example.velvetaura.repository.ClienteRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    // Tabla hash: la clave es la identificación del cliente (búsqueda y duplicados en O(1))
    private final Map<String, Cliente> clientes = new HashMap<>();

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // Al iniciar se llena el HashMap con los clientes guardados en PostgreSQL
    @PostConstruct
    public void cargarClientes() {
        for (Cliente c : repository.findAll()) {
            clientes.put(c.getIdentificacion(), c);
        }
    }

    private String normalizar(String identificacion) {
        return identificacion == null ? "" : identificacion.trim();
    }

    // F05
    public Cliente registrar(Cliente cliente) {
        String identificacion = normalizar(cliente.getIdentificacion());
        if (clientes.containsKey(identificacion)) {
            throw new IllegalArgumentException("Ya existe un cliente con la identificación " + identificacion);
        }
        cliente.setIdentificacion(identificacion);
        Cliente guardado = repository.save(cliente);
        clientes.put(identificacion, guardado);
        return guardado;
    }

    // Lo usa el pedido (F06) para comprobar que el cliente existe
    public Optional<Cliente> buscarPorIdentificacion(String identificacion) {
        return Optional.ofNullable(clientes.get(normalizar(identificacion)));
    }

    public List<Cliente> listar() {
        List<Cliente> resultado = new ArrayList<>(clientes.values());
        resultado.sort(Comparator.comparing(Cliente::getNombre));
        return resultado;
    }
}
