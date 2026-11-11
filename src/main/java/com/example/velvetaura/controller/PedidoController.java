package com.example.velvetaura.controller;

import com.example.velvetaura.model.Pedido;
import com.example.velvetaura.model.Prioridad;
import com.example.velvetaura.service.ClienteService;
import com.example.velvetaura.service.PedidoService;
import com.example.velvetaura.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProductoService productoService;

    public PedidoController(PedidoService pedidoService,
                            ClienteService clienteService,
                            ProductoService productoService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.productoService = productoService;
    }

    @GetMapping
    public String pagina(Model model) {
        model.addAttribute("pedidos", pedidoService.listarPedidos());
        model.addAttribute("cola", pedidoService.verColaOrdenada());
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("productos", productoService.listar(null));
        model.addAttribute("prioridades", Prioridad.values());
        return "pedidos";
    }


    @PostMapping("/crear")
    public String crear(@RequestParam String identificacion,
                        @RequestParam Prioridad prioridad,
                        @RequestParam(required = false) List<String> skus,
                        @RequestParam(required = false) List<String> cantidades,
                        RedirectAttributes redirect) {
        try {
            List<PedidoService.ItemPedido> items = new ArrayList<>();
            if (skus != null) {
                for (int i = 0; i < skus.size(); i++) {
                    String sku = skus.get(i) == null ? "" : skus.get(i).trim();
                    String texto = (cantidades != null && i < cantidades.size() && cantidades.get(i) != null)
                            ? cantidades.get(i).trim() : "";
                    if (sku.isEmpty() && texto.isEmpty()) {
                        continue; // fila vacía
                    }
                    if (sku.isEmpty()) {
                        throw new IllegalArgumentException("Falta el SKU en una de las filas");
                    }
                    int cantidad;
                    try {
                        cantidad = Integer.parseInt(texto);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cantidad inválida para el SKU " + sku);
                    }
                    items.add(new PedidoService.ItemPedido(sku, cantidad));
                }
            }
            Pedido pedido = pedidoService.crearPedido(identificacion, prioridad, items);
            redirect.addFlashAttribute("mensaje", "Pedido #" + pedido.getId() + " creado correctamente");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }


    @PostMapping("/atender")
    public String atender(RedirectAttributes redirect) {
        try {
            Pedido pedido = pedidoService.atenderSiguiente();
            redirect.addFlashAttribute("mensaje", "Se empezó a preparar el pedido #" + pedido.getId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }


    @PostMapping("/{id}/avanzar")
    public String avanzar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Pedido pedido = pedidoService.avanzarEstado(id);
            redirect.addFlashAttribute("mensaje",
                    "El pedido #" + pedido.getId() + " ahora está " + pedido.getEstado());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }


    @PostMapping("/deshacer")
    public String deshacer(RedirectAttributes redirect) {
        try {
            Pedido pedido = pedidoService.deshacerUltimoCambio();
            redirect.addFlashAttribute("mensaje",
                    "Se deshizo el cambio: el pedido #" + pedido.getId() + " volvió a " + pedido.getEstado());
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pedidos";
    }
}
