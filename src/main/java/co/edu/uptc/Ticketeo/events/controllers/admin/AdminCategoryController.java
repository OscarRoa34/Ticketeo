package co.edu.uptc.Ticketeo.events.controllers.admin;

import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final int PAGE_SIZE = 6;

    private final EventCategoryService eventCategoryService;

    @GetMapping({"", "/"})
    public String showCategories(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<EventCategory> categoryPage = eventCategoryService.getCategoriesPaginated(page, PAGE_SIZE);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        return "adminCategories";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new EventCategory());
        return "adminCategoryForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", eventCategoryService.getEventCategoryById(id));
        return "adminCategoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute EventCategory category) {
        eventCategoryService.saveCategory(category);
        return "redirect:/admin/category";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Integer id) {
        eventCategoryService.deleteCategory(id);
        return "redirect:/admin/category";
    }
}