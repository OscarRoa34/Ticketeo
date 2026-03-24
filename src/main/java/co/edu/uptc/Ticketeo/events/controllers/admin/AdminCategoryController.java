package co.edu.uptc.Ticketeo.events.controllers.admin;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import lombok.RequiredArgsConstructor;

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
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            eventCategoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Categoría eliminada correctamente.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "No se pudo eliminar la categoría. Verifica si tiene eventos asociados e inténtalo de nuevo."
            );
        }
        return "redirect:/admin/category";
    }
}