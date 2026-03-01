package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.services.EventService;
import co.edu.uptc.Ticketeo.services.InterestReportService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String showUserHome(
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        int pageSize = 6;
        Page eventPage = eventService.getEventsPaginated(page, pageSize);

        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("carouselEvents", eventService.getRandomEvents(5));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("totalItems", eventPage.getTotalElements());

        return "userHome";
    }
}
