package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class PublicViewHomeController {

    private static final int PAGE_SIZE = 6;

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;

    @GetMapping
    public String showUserHome(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false, defaultValue = "date_desc") String sort,
            Authentication authentication,
            Model model) {

        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "redirect:/admin";
        }

        Page<Event> eventPage = eventService.getEventsFiltered(search, categoryId, page, PAGE_SIZE, sort);
        List<Event> carouselEvents = eventService.getRandomEvents(5);

        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("carouselEvents", carouselEvents);
        model.addAttribute("availableTicketsByEventId", buildAvailabilityMap(eventPage.getContent(), carouselEvents));
        model.addAttribute("completedEventsById", buildCompletedMap(eventPage.getContent(), carouselEvents));
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("totalItems", eventPage.getTotalElements());
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("currentSort", sort);

        return "user/userHome";
    }

    private Map<Integer, Boolean> buildAvailabilityMap(List<Event> listedEvents, List<Event> carouselEvents) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        mergeAvailability(result, listedEvents);
        mergeAvailability(result, carouselEvents);
        return result;
    }

    private void mergeAvailability(Map<Integer, Boolean> target, List<Event> events) {
        for (Event event : events) {
            if (event == null || event.getId() == null || target.containsKey(event.getId())) {
                continue;
            }
            target.put(event.getId(), eventService.hasAvailableTicketsForEvent(event.getId()));
        }
    }

    private Map<Integer, Boolean> buildCompletedMap(List<Event> listedEvents, List<Event> carouselEvents) {
        Map<Integer, Boolean> result = new LinkedHashMap<>();
        mergeCompleted(result, listedEvents);
        mergeCompleted(result, carouselEvents);
        return result;
    }

    private void mergeCompleted(Map<Integer, Boolean> target, List<Event> events) {
        for (Event event : events) {
            if (event == null || event.getId() == null || target.containsKey(event.getId())) {
                continue;
            }
            target.put(event.getId(), eventService.isCompletedEvent(event));
        }
    }
}
