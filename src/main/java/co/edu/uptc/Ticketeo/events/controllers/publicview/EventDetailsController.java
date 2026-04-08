package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventDetailsController {

    private final EventService eventService;
    private final InterestReportService interestReportService;
    private final UserRepository userRepository;


    @GetMapping("/{id}")
    public String viewEventDetails(@PathVariable Integer id, Model model, Authentication authentication) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }

        boolean isCompletedEvent = eventService.isCompletedEvent(event);
        boolean hasAvailableTicketsForEvent = eventService.hasAvailableTicketsForEvent(id);

        boolean isInterested = false;
        if (authentication != null && authentication.isAuthenticated()) {
            Long userId = userRepository.findUserIdByUsername(authentication.getName()).orElse(null);
            if (userId != null) {
                isInterested = interestReportService.isUserInterested(id, userId);
            }
        }

        model.addAttribute("event", event);
        model.addAttribute("isInterested", isInterested);
        model.addAttribute("isCompletedEvent", isCompletedEvent);
        model.addAttribute("hasAvailableTicketsForEvent", hasAvailableTicketsForEvent);
        return "events/eventDetails";
    }

    @PostMapping(value = "/{id}/interest", headers = "X-Requested-With=XMLHttpRequest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registerInterestAjax(@PathVariable Integer id, Authentication authentication) {
        Event event = eventService.getEventById(id);

        if (event == null || authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Debes iniciar sesión para gestionar tu interés"
            ));
        }

        if (eventService.isCompletedEvent(event)) {
            return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "message", "No puedes mostrar interés en un evento completado."
            ));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "No se pudo identificar el usuario"
            ));
        }

        boolean interestedNow = interestReportService.toggleInterest(event, user);
        String message = interestedNow
                ? "¡Genial! Hemos registrado tu interés en " + event.getName()
                : "Ya no estás interesado en " + event.getName();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "interested", interestedNow,
                "message", message
        ));
    }

    @PostMapping("/{id}/interest")
    public String registerInterest(@PathVariable Integer id, RedirectAttributes redirectAttributes, Authentication authentication) {
        Event event = eventService.getEventById(id);

        if (event != null && eventService.isCompletedEvent(event)) {
            redirectAttributes.addFlashAttribute("errorMessage", "No puedes mostrar interés en un evento completado.");
            return "redirect:/event/" + id;
        }

        if (event != null && authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                interestReportService.toggleInterest(event, user);
            }
        }

        return "redirect:/event/" + id;
    }
}