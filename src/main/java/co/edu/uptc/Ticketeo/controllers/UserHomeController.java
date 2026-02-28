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

@Controller
@RequestMapping("/user")
public class UserHomeController {

    private final EventService eventService;
    private final InterestReportService interestReportService;

    public UserHomeController(EventService eventService, InterestReportService interestReportService) {
        this.eventService = eventService;
        this.interestReportService= interestReportService;
    }
    @GetMapping
    public String showUserHome(Model model) {
        model.addAttribute("eventos", eventService.getAllEvents());
        return "userHome";
    }

    @PostMapping("/interesar/{id}")
    public String registrarInteres(@PathVariable("id") Integer id) {
        Event evento = eventService.getEventById(id);
        if (evento != null) {
            interestReportService.registrarInteres(evento);
        }
        return "redirect:/user";
    }
}
