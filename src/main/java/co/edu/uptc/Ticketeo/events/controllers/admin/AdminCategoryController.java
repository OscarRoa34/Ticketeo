package co.edu.uptc.Ticketeo.events.controllers.admin;

import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private static final int PAGE_SIZE = 6;
    private static final String REDIRECT_CATEGORY_PATH = "redirect:/admin/category";

    private final EventCategoryService eventCategoryService;

    @GetMapping({"", "/"})
    public String showCategories(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<EventCategory> categoryPage = eventCategoryService.getCategoriesPaginated(page, PAGE_SIZE);
        model.addAttribute("categories", categoryPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        return "events/adminCategories";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new EventCategory());
        return "events/adminCategoryForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", eventCategoryService.getEventCategoryById(id));
        return "events/adminCategoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute EventCategory category) {
        eventCategoryService.saveCategory(category);
        return REDIRECT_CATEGORY_PATH;
    }

    @GetMapping("/delete/{id}")
    public Object deleteCategory(@PathVariable Integer id,
                                 @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                 RedirectAttributes redirectAttributes) {
        try {
            eventCategoryService.deleteCategory(id);
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.ok().build();
            }
            return REDIRECT_CATEGORY_PATH;
        } catch (IllegalStateException ex) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return REDIRECT_CATEGORY_PATH;
        }
    }
}