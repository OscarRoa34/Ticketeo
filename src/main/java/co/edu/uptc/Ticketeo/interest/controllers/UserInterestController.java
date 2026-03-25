package co.edu.uptc.Ticketeo.interest.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.interest.services.InterestReportService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller
@RequestMapping("/user/interests")
@RequiredArgsConstructor
public class UserInterestController {

    private final InterestReportService interestReportService;

    @GetMapping
    public String showUserInterests(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<Event> interestedEvents = interestReportService.getUserInterests(username);
        model.addAttribute("events", interestedEvents);
        return "interest/userInterests";
    }
}

