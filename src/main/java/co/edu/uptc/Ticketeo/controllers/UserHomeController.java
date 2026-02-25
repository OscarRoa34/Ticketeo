package co.edu.uptc.Ticketeo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.uptc.Ticketeo.services.EventService;

@Controller
@RequestMapping("/user")
public class UserHomeController {

    private final EventService eventService;

    public UserHomeController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public String showUserHome(Model model) {
        model.addAttribute("eventos", eventService.getAllEvents());
        return "userHome";
    }
}
