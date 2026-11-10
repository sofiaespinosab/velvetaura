package com.example.velvetaura.service;

import com.example.velvetaura.estructuras.Lista;
import com.example.velvetaura.model.Evento;
import com.example.velvetaura.model.TipoEvento;
import com.example.velvetaura.repository.EventoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository repository;

    // Lista enlazada propia: el evento más reciente siempre queda de primero
    private final Lista<Evento> historial = new Lista<>();

    public EventoService(EventoRepository repository) {
        this.repository = repository;
    }

    // Al iniciar se cargan los eventos guardados en PostgreSQL, del más antiguo al más nuevo,
    // y como se insertan al inicio, el último en entrar (el más reciente) queda de primero.
    @PostConstruct
    public void cargarHistorial() {
        for (Evento e : repository.findAllByOrderByIdAsc()) {
            historial.agregarAlInicio(e);
        }
    }

    // Lo llaman los demás servicios cuando pasa algo importante, por ejemplo:
    // eventoService.registrar(TipoEvento.PEDIDO_CREADO, "Pedido #5 creado para el cliente 123");
    public Evento registrar(TipoEvento tipo, String descripcion) {
        Evento guardado = repository.save(new Evento(tipo, descripcion));
        historial.agregarAlInicio(guardado);
        return guardado;
    }

    // F11: del más reciente al más antiguo
    public List<Evento> listar() {
        return historial.aLista();
    }
}
