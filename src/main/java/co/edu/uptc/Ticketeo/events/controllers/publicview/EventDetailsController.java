package co.edu.uptc.Ticketeo.events.controllers.publicview;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.interest.services.InterestReportService;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositorys.UserRepository;
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
        
        boolean isInterested = false;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                isInterested = interestReportService.isUserInterested(id, user.getId());
            }
        }
        
        model.addAttribute("event", event);
        model.addAttribute("isInterested", isInterested);
        return "eventDetails";
    }

    @PostMapping("/{id}/interest")
    public String registerInterest(@PathVariable Integer id, RedirectAttributes redirectAttributes, Authentication authentication) {
        Event event = eventService.getEventById(id);

        if (event != null && authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                boolean interestedNow = interestReportService.toggleInterest(event, user);
                if (interestedNow) {
                    redirectAttributes.addFlashAttribute("successMessage", "¡Genial! Hemos registrado tu interés en " + event.getName());
                } else {
                    redirectAttributes.addFlashAttribute("successMessage", "Ya no estás interesado en " + event.getName());
                }
            }
        }

        return "redirect:/event/" + id;
    }
}