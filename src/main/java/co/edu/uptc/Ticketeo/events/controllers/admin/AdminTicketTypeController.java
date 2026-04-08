package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/ticket-type")
@RequiredArgsConstructor
public class AdminTicketTypeController {

    private static final int PAGE_SIZE = 6;
    private static final String FLASH_SUCCESS_MESSAGE = "successMessage";
    private static final String FLASH_ERROR_MESSAGE = "errorMessage";

    private final TicketTypeService ticketTypeService;

    @Value("${app.routes.admin.ticket-type:/admin/ticket-type}")
    private String ticketTypeRoute = "/admin/ticket-type";

    @Value("${app.views.admin.ticket-type.list:events/adminTicketTypes}")
    private String ticketTypeListView = "events/adminTicketTypes";

    @Value("${app.views.admin.ticket-type.form:events/adminTicketTypeForm}")
    private String ticketTypeFormView = "events/adminTicketTypeForm";

    @GetMapping({"", "/"})
    public String showTicketTypes(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<TicketType> ticketTypePage = ticketTypeService.getTicketTypesPaginated(page, PAGE_SIZE);
        model.addAttribute("ticketTypes", ticketTypePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketTypePage.getTotalPages());
        return ticketTypeListView;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("ticketType", new TicketTypeForm());
        addExistingTicketTypeNames(model, null);
        return ticketTypeFormView;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        TicketType ticketType = ticketTypeService.getTicketTypeById(id);
        TicketTypeForm form = new TicketTypeForm();
        if (ticketType != null) {
            form.setId(ticketType.getId());
            form.setName(ticketType.getName());
        }
        model.addAttribute("ticketType", form);
        addExistingTicketTypeNames(model, form.getId());
        return ticketTypeFormView;
    }

    @PostMapping("/save")
    public String saveTicketType(@ModelAttribute("ticketType") TicketTypeForm ticketTypeForm,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        boolean isNew = ticketTypeForm.getId() == null;
        TicketType ticketType = new TicketType();
        ticketType.setId(ticketTypeForm.getId());
        ticketType.setName(ticketTypeForm.getName());
        try {
            ticketTypeService.saveTicketType(ticketType);
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, isNew
                    ? "Tipo de ticket creado correctamente."
                    : "Tipo de ticket actualizado correctamente.");
            return buildRedirectPath();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute(FLASH_ERROR_MESSAGE, ex.getMessage());
        } catch (RuntimeException ex) {
            model.addAttribute(FLASH_ERROR_MESSAGE, "No fue posible guardar el tipo de ticket.");
        }
        model.addAttribute("ticketType", ticketTypeForm);
        addExistingTicketTypeNames(model, ticketTypeForm.getId());
        return ticketTypeFormView;
    }

    @GetMapping("/delete/{id}")
    public Object deleteTicketType(@PathVariable Integer id,
                                   @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                                   RedirectAttributes redirectAttributes) {
        try {
            ticketTypeService.deleteTicketType(id);
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.ok("Tipo de ticket eliminado correctamente.");
            }
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, "Tipo de ticket eliminado correctamente.");
            return buildRedirectPath();
        } catch (RuntimeException ex) {
            String errorMessage = "No fue posible eliminar el tipo de ticket.";
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, errorMessage);
            return buildRedirectPath();
        }
    }

    private String buildRedirectPath() {
        return "redirect:" + ticketTypeRoute;
    }

    private void addExistingTicketTypeNames(Model model, Integer excludedId) {
        List<String> existingNames = ticketTypeService.getTicketTypeNamesExcludingId(excludedId);
        model.addAttribute("existingTicketTypeNames", existingNames == null ? List.of() : existingNames);
    }

    public static class TicketTypeForm {
        private Integer id;
        private String name;

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
    }
}
