package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String EVENT_NEW_RETURN_PATH = "/admin/event/new";
    private static final String EVENT_NEW_DRAFT_RETURN_PATH = "/admin/event/new?draft=true";
    private static final Pattern EVENT_EDIT_RETURN_PATTERN = Pattern.compile("^/admin/event/edit/(\\d+)$");

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
        EventFormReturnTarget returnTarget = parseEventFormReturnTarget(returnTo);
        model.addAttribute("returnTo", toCanonicalEventFormPath(returnTarget));
        return "events/adminCategoryForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               Model model) {
        model.addAttribute("category", eventCategoryService.getEventCategoryById(id));
        EventFormReturnTarget returnTarget = parseEventFormReturnTarget(returnTo);
        model.addAttribute("returnTo", toCanonicalEventFormPath(returnTarget));
        return "events/adminCategoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute EventCategory category,
                               @RequestParam(value = "returnTo", required = false) String returnTo,
                               RedirectAttributes redirectAttributes) {
        boolean isNew = category.getId() == null;
        EventFormReturnTarget returnTarget = parseEventFormReturnTarget(returnTo);
        EventCategory savedCategory = eventCategoryService.saveCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", isNew
                ? "Categoria creada correctamente."
                : "Categoria actualizada correctamente.");
        return buildPostSaveRedirectPath(returnTarget, savedCategory != null ? savedCategory.getId() : null);
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

    private String buildPostSaveRedirectPath(EventFormReturnTarget returnTarget, Integer categoryId) {
        if (returnTarget == null) {
            return REDIRECT_CATEGORY_PATH;
        }

        String canonicalPath = toCanonicalEventFormPath(returnTarget);
        if (canonicalPath == null) {
            return REDIRECT_CATEGORY_PATH;
        }

        if (categoryId == null) {
            return "redirect:" + canonicalPath;
        }

        String separator = canonicalPath.contains("?") ? "&" : "?";
        return "redirect:" + canonicalPath + separator + "selectedCategoryId=" + categoryId;
    }

    private EventFormReturnTarget parseEventFormReturnTarget(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return null;
        }

        String trimmedPath = returnTo.trim();
        if (EVENT_NEW_RETURN_PATH.equals(trimmedPath)) {
            return new EventFormReturnTarget(null, false);
        }
        if (EVENT_NEW_DRAFT_RETURN_PATH.equals(trimmedPath)) {
            return new EventFormReturnTarget(null, true);
        }

        Matcher matcher = EVENT_EDIT_RETURN_PATTERN.matcher(trimmedPath);
        if (!matcher.matches()) {
            return null;
        }

        try {
            int eventId = Integer.parseInt(matcher.group(1));
            if (eventId <= 0) {
                return null;
            }
            return new EventFormReturnTarget(eventId, false);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toCanonicalEventFormPath(EventFormReturnTarget returnTarget) {
        if (returnTarget == null) {
            return null;
        }

        if (returnTarget.eventId != null) {
            return "/admin/event/edit/" + returnTarget.eventId;
        }

        return returnTarget.draft ? EVENT_NEW_DRAFT_RETURN_PATH : EVENT_NEW_RETURN_PATH;
    }

    private static final class EventFormReturnTarget {
        private final Integer eventId;
        private final boolean draft;

        private EventFormReturnTarget(Integer eventId, boolean draft) {
            this.eventId = eventId;
            this.draft = draft;
        }
    }
}