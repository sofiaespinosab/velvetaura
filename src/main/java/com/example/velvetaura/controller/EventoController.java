package com.example.velvetaura.controller;

import com.example.velvetaura.service.EventoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/eventos")
public class EventoController {

    private final EventoService service;

    public EventoController(EventoService service) {
        this.service = service;
    }


    @GetMapping
    public String listar(Model model) {
        model.addAttribute("eventos", service.listar());
        return "eventos";
    }
}
