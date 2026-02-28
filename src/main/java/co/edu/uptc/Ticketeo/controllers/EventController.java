package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.services.EventService;
import co.edu.uptc.Ticketeo.services.InterestReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/event")
public class EventController {

    private final EventService eventService;
    private final InterestReportService interestReportService;

    public EventController(EventService eventService, InterestReportService interestReportService) {
        this.eventService = eventService;
        this.interestReportService = interestReportService;
    }

    @GetMapping("/{id}")
    public String viewEventDetails(@PathVariable Integer id, Model model) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }
        model.addAttribute("event", event);
        return "eventDetails";
    }

    @PostMapping("/{id}/interest")
    public String registerInterest(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Event event = eventService.getEventById(id);

        if (event != null) {
            interestReportService.registerInterest(event);
            redirectAttributes.addFlashAttribute("successMessage", "¡Genial! Hemos registrado tu interés en " + event.getName());
        }

        return "redirect:/event/" + id;
    }
}