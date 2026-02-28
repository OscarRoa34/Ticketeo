package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.services.EventService;
import co.edu.uptc.Ticketeo.services.InterestReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import co.edu.uptc.Ticketeo.services.EventCategoryService;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final InterestReportService interestReportService;

    public AdminPageController(EventService eventService, EventCategoryService eventCategoryService, InterestReportService interestReportService) {
        this.eventService = eventService;
        this.eventCategoryService = eventCategoryService;
        this.interestReportService = interestReportService;
    }

    @GetMapping
    public String showAdminPage(Model model) {
        model.addAttribute("events", eventService.getAllEvents());
        return "admin";
    }

    @GetMapping("/event/new")
    public String showEventForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        return "eventForm";
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Event event = eventService.getEventById(id);

        if (event != null) {
            model.addAttribute("event", event);
            model.addAttribute("categories", eventCategoryService.getAllCategories());
            return "eventForm";
        }
        return "redirect:/admin";
    }

    @PostMapping("/event/save")
    public String saveEvent(@ModelAttribute Event event,
                            @RequestParam("imageFile") MultipartFile image) {

        if (!image.isEmpty()) {
            Path imageDirectory = Paths.get("src/main/resources/static/uploads");
            String absolutePath = imageDirectory.toFile().getAbsolutePath();

            try {
                if (!Files.exists(imageDirectory)) {
                    Files.createDirectories(imageDirectory);
                }

                byte[] imageBytes = image.getBytes();
                Path fullPath = Paths.get(absolutePath + "/" + image.getOriginalFilename());
                Files.write(fullPath, imageBytes);

                event.setImageUrl("/uploads/" + image.getOriginalFilename());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (event.getId() != null) {
                Event existingEvent = eventService.getEventById(event.getId());
                if (existingEvent != null) {
                    event.setImageUrl(existingEvent.getImageUrl());
                }
            }
        }

        eventService.saveEvent(event);

        return "redirect:/admin";
    }

    @GetMapping("/event/delete/{id}")
    public String deleteEvent(@PathVariable Integer id) {
        eventService.deleteEvent(id);
        return "redirect:/admin";
    }

    @GetMapping("/reportes")
    public String showReports(Model model) {
        model.addAttribute("interestRanking", interestReportService.getEventInterestRanking());
        return "reports";
    }
}