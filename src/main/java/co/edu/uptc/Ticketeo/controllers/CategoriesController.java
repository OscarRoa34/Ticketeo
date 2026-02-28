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
@RequestMapping("/admin/category")
public class CategoriesController {

    private final EventCategoryService eventCategoryService;

    public CategoriesController(EventCategoryService eventCategoryService) {
        this.eventCategoryService = eventCategoryService;
    }

    @GetMapping({"", "/"})
    public String listCategories(Model model) {
        List<EventCategory> list = eventCategoryService.getAllCategories();
        model.addAttribute("categories", list);

        return "categories";
    }

    @GetMapping("/new")
    public String showNewCategoryForm(Model model) {
        model.addAttribute("category", new EventCategory());
        return "categoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute EventCategory category) {
        eventCategoryService.saveCategory(category);
        return "redirect:/admin/category";
    }

    @GetMapping("/edit/{id}")
    public String showEditCategoryForm(@PathVariable("id") Integer id, Model model) {
        EventCategory category = eventCategoryService.getEventCategoryById(id);
        model.addAttribute("category", category);

        return "categoryForm";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        eventCategoryService.deleteCategory(id);
        return "redirect:/admin/category";
    }
}