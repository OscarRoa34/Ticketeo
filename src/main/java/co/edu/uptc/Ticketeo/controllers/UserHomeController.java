package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.services.EventCategoryService;
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
    private final EventCategoryService eventCategoryService;

    public UserHomeController(EventService eventService, InterestReportService interestReportService, EventCategoryService eventCategoryService) {
        this.eventService = eventService;
        this.interestReportService = interestReportService;
        this.eventCategoryService = eventCategoryService;
    }

    @GetMapping
    public String showUserHome(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "date_desc") String sort,
            Model model) {

        int pageSize = 6;
        Page<Event> eventPage = eventService.getEventsFiltered(search, categoryId, page, pageSize, sort);

        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("carouselEvents", eventService.getRandomEvents(5));
        model.addAttribute("categories", eventCategoryService.getAllCategories());

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("totalItems", eventPage.getTotalElements());

        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("currentSort", sort);

        return "userHome";
    }
}
