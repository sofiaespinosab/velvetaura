package com.example.velvetaura.controller;

import com.example.velvetaura.model.Cliente;
import com.example.velvetaura.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", service.listar());
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        return "clientes";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("cliente") Cliente cliente,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.registrar(cliente);
                redirect.addFlashAttribute("mensaje", "Cliente registrado correctamente");
                return "redirect:/clientes";
            } catch (IllegalArgumentException e) {
                result.rejectValue("identificacion", "duplicado", e.getMessage());
            }
        }
        model.addAttribute("clientes", service.listar());
        return "clientes";
    }
}
