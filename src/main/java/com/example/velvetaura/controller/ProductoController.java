package com.example.velvetaura.controller;

import com.example.velvetaura.model.Categoria;
import com.example.velvetaura.model.Producto;
import com.example.velvetaura.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    private void cargarPagina(Model model, Categoria categoria) {
        model.addAttribute("productos", service.listar(categoria));
        model.addAttribute("categorias", Categoria.values());
        model.addAttribute("categoriaSeleccionada", categoria);
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Categoria categoria, Model model) {
        cargarPagina(model, categoria);
        if (!model.containsAttribute("producto")) {
            model.addAttribute("producto", new Producto());
        }
        return "productos";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("producto") Producto producto,
                        BindingResult result,
                        Model model,
                        RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.registrar(producto);
                redirect.addFlashAttribute("mensaje", "Producto registrado correctamente");
                return "redirect:/productos";
            } catch (IllegalArgumentException e) {
                result.rejectValue("sku", "duplicado", e.getMessage());
            }
        }
        cargarPagina(model, null);
        return "productos";
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String sku, Model model) {
        Optional<Producto> encontrado = service.buscarPorSku(sku);
        if (encontrado.isPresent()) {
            model.addAttribute("resultado", encontrado.get());
        } else {
            model.addAttribute("error", "Producto no encontrado");
        }
        cargarPagina(model, null);
        model.addAttribute("producto", new Producto());
        return "productos";
    }

    @PostMapping("/reponer")
    public String reponer(@RequestParam String sku,
                          @RequestParam int cantidad,
                          RedirectAttributes redirect) {
        try {
            service.reponerStock(sku, cantidad);
            redirect.addFlashAttribute("mensaje", "Stock actualizado");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }
}
