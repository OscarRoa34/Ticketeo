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

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/ticket-type")
@RequiredArgsConstructor
public class AdminTicketTypeController {

    private static final int PAGE_SIZE = 6;

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
    public String saveTicketType(@ModelAttribute TicketType ticketType) {
        ticketTypeService.saveTicketType(ticketType);
        return "redirect:/admin/ticket-type";
    }

    @GetMapping("/delete/{id}")
    public String deleteTicketType(@PathVariable Integer id) {
        ticketTypeService.deleteTicketType(id);
        return "redirect:/admin/ticket-type";
    }
}
