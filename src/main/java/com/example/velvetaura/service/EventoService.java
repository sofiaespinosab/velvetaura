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


    private final Lista<Evento> historial = new Lista<>();

    public EventoService(EventoRepository repository) {
        this.repository = repository;
    }


    @PostConstruct
    public void cargarHistorial() {
        for (Evento e : repository.findAllByOrderByIdAsc()) {
            historial.agregarAlInicio(e);
        }
    }


    public Evento registrar(TipoEvento tipo, String descripcion) {
        Evento guardado = repository.save(new Evento(tipo, descripcion));
        historial.agregarAlInicio(guardado);
        return guardado;
    }


    public List<Evento> listar() {
        return historial.aLista();
    }
}
