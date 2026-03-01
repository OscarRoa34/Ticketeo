package co.edu.uptc.Ticketeo.controllers;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String listCategories(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 6;
        Page<EventCategory> categoryPage = eventCategoryService.getCategoriesPaginated(page, pageSize);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
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