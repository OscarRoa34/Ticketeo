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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/ticket-type")
@RequiredArgsConstructor
public class AdminTicketTypeController {

    private static final int PAGE_SIZE = 6;
    private static final String REDIRECT_TICKET_TYPE_PATH = "redirect:/admin/ticket-type";

    private final TicketTypeService ticketTypeService;

    @GetMapping({"", "/"})
    public String showTicketTypes(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<TicketType> ticketTypePage = ticketTypeService.getTicketTypesPaginated(page, PAGE_SIZE);
        model.addAttribute("ticketTypes", ticketTypePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketTypePage.getTotalPages());
        return "events/adminTicketTypes";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("ticketType", new TicketType());
        return "events/adminTicketTypeForm";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("ticketType", ticketTypeService.getTicketTypeById(id));
        return "events/adminTicketTypeForm";
    }

    @PostMapping("/save")
    public String saveTicketType(@ModelAttribute TicketType ticketType, RedirectAttributes redirectAttributes) {
        boolean isNew = ticketType.getId() == null;
        try {
            ticketTypeService.saveTicketType(ticketType);
            redirectAttributes.addFlashAttribute("successMessage", isNew
                    ? "Tipo de ticket creado correctamente."
                    : "Tipo de ticket actualizado correctamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "No fue posible guardar el tipo de ticket.");
        }
        return REDIRECT_TICKET_TYPE_PATH;
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
            redirectAttributes.addFlashAttribute("successMessage", "Tipo de ticket eliminado correctamente.");
            return REDIRECT_TICKET_TYPE_PATH;
        } catch (RuntimeException ex) {
            String errorMessage = "No fue posible eliminar el tipo de ticket.";
            if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
            return REDIRECT_TICKET_TYPE_PATH;
        }
    }
}
