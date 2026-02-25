package co.edu.uptc.Ticketeo.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.uptc.Ticketeo.models.EventCategory;
import co.edu.uptc.Ticketeo.services.EventCategoryService;

@Controller
@RequestMapping("/admin/categoria") 
public class CategoriesController {

    private final EventCategoryService categoriaService;

    public CategoriesController(EventCategoryService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping({"", "/"})
    public String listarCategorias(Model model) {
        List<EventCategory> lista = categoriaService.getAllCategories();
        model.addAttribute("categorias", lista);
        
        return "categories";
    }

    @GetMapping("/nueva")
    public String mostrarFormularioNuevaCategoria(Model model) {
        model.addAttribute("categoria", new EventCategory());
        return "categoria-form"; 
    }

    @PostMapping("/guardar")
    public String guardarCategoria(@ModelAttribute EventCategory categoria) {
        categoriaService.saveCategory(categoria);
        
        return "redirect:/admin/categoria"; 
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarCategoria(@PathVariable("id") Integer id, Model model) {
        EventCategory categoria = categoriaService.getEventCategoryById(id);
        model.addAttribute("categoria", categoria);
        
        return "categoria-form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable("id") Integer id) {
        categoriaService.deleteCategory(id);
        return "redirect:/admin/categoria";
    }
}