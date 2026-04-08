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
    private static final String SELECTED_CATEGORY_PARAM = "selectedCategoryId";

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
    public String showCreateForm(@RequestParam(value = "fromEventForm", defaultValue = "false") boolean fromEventForm,
                                 @RequestParam(value = "eventId", required = false) Integer eventId,
                                 @RequestParam(value = "draft", defaultValue = "false") boolean draft,
                                 Model model) {
        model.addAttribute("category", new CategoryForm());
        EventFormNavigation.populateFormContext(model, fromEventForm, eventId, draft, "/admin/category");
        return "events/adminCategoryForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               @RequestParam(value = "fromEventForm", defaultValue = "false") boolean fromEventForm,
                               @RequestParam(value = "eventId", required = false) Integer eventId,
                               @RequestParam(value = "draft", defaultValue = "false") boolean draft,
                               Model model) {
        EventCategory category = eventCategoryService.getEventCategoryById(id);
        CategoryForm form = new CategoryForm();
        if (category != null) {
            form.setId(category.getId());
            form.setName(category.getName());
            form.setColor(category.getColor());
        }
        model.addAttribute("category", form);
        EventFormNavigation.populateFormContext(model, fromEventForm, eventId, draft, "/admin/category");
        return "events/adminCategoryForm";
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute("category") CategoryForm categoryForm,
                               @RequestParam(value = "fromEventForm", defaultValue = "false") boolean fromEventForm,
                               @RequestParam(value = "eventId", required = false) Integer eventId,
                               @RequestParam(value = "draft", defaultValue = "false") boolean draft,
                               RedirectAttributes redirectAttributes) {
        boolean isNew = categoryForm.getId() == null;
        EventCategory category = new EventCategory();
        category.setId(categoryForm.getId());
        category.setName(categoryForm.getName());
        category.setColor(categoryForm.getColor());
        EventCategory savedCategory = eventCategoryService.saveCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", isNew
                ? "Categoria creada correctamente."
                : "Categoria actualizada correctamente.");
        return EventFormNavigation.resolvePostSaveRedirect(fromEventForm, eventId, draft, SELECTED_CATEGORY_PARAM,
                savedCategory != null ? savedCategory.getId() : null, REDIRECT_CATEGORY_PATH);
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

    public static class CategoryForm {
        private Integer id;
        private String name;
        private String color;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}
