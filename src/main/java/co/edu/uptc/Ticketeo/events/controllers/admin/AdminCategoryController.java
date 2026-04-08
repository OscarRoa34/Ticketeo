package co.edu.uptc.Ticketeo.events.controllers.admin;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private static final String REDIRECT_CATEGORY_PATH = "redirect:/admin/category";
    private static final String EVENT_FORM_RETURN_PREFIX = "/admin/event/";

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
    public String showCreateForm(@RequestParam(value = "returnTo", required = false) String returnTo,
                                 Model model) {
        model.addAttribute("category", new EventCategory());
        model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
        return "events/adminCategoryForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               Model model) {
        model.addAttribute("category", eventCategoryService.getEventCategoryById(id));
        model.addAttribute("returnTo", sanitizeReturnTo(returnTo));
        return "events/adminCategoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute EventCategory category,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               RedirectAttributes redirectAttributes) {
        boolean isNew = category.getId() == null;
        String sanitizedReturnTo = sanitizeReturnTo(returnTo);
        EventCategory savedCategory = eventCategoryService.saveCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", isNew
                ? "Categoria creada correctamente."
                : "Categoria actualizada correctamente.");
        return buildPostSaveRedirectPath(sanitizedReturnTo, savedCategory != null ? savedCategory.getId() : null);
    }

    @GetMapping("/delete/{id}")
    public Object deleteCategory(@PathVariable Integer id,
                                 @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                 RedirectAttributes redirectAttributes) {
        try {
            eventCategoryService.deleteCategory(id);
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.ok("Categoria eliminada correctamente.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Categoria eliminada correctamente.");
            return REDIRECT_CATEGORY_PATH;
        } catch (IllegalStateException ex) {
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return REDIRECT_CATEGORY_PATH;
        }
    }

    private String buildPostSaveRedirectPath(String returnTo, Integer categoryId) {
        if (returnTo == null || returnTo.isBlank()) {
            return REDIRECT_CATEGORY_PATH;
        }

        if (categoryId == null) {
            return "redirect:" + returnTo;
        }

        String separator = returnTo.contains("?") ? "&" : "?";
        return "redirect:" + returnTo + separator + "selectedCategoryId=" + categoryId;
    }

    private String sanitizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return null;
        }

        String trimmedPath = returnTo.trim();
        if (!trimmedPath.startsWith(EVENT_FORM_RETURN_PREFIX)
                || trimmedPath.startsWith("//")
                || trimmedPath.contains("://")
                || trimmedPath.contains("\n")
                || trimmedPath.contains("\r")) {
            return null;
        }

        return trimmedPath;
    }
}