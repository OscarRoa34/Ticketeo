package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.services.EventService;
import co.edu.uptc.Ticketeo.services.InterestReportService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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
    public String showAdminPage(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 6;
        Page<Event> eventPage = eventService.getEventsPaginated(page, pageSize);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        return "admin";
    }

    @GetMapping("/trash")
    public String showTrash(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 6;
        Page<Event> eventPage = eventService.getInactiveEventsPaginated(page, pageSize);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        return "adminTrash";
    }

    @GetMapping("/event/new")
    public String showEventForm(@RequestParam(defaultValue = "false") boolean draft, Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("draft", draft);
        return "eventForm";
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id,
                               @RequestParam(defaultValue = "false") boolean fromTrash,
                               Model model) {
        Event event = eventService.getEventById(id);
        if (event != null) {
            model.addAttribute("event", event);
            model.addAttribute("categories", eventCategoryService.getAllCategories());
            model.addAttribute("draft", fromTrash);
            return "eventForm";
        }
        return "redirect:/admin";
    }

    @PostMapping("/event/save")
    public String saveEvent(@ModelAttribute Event event,
                            @RequestParam("imageFile") MultipartFile image,
                            @RequestParam(value = "category", required = false) Integer categoryId,
                            @RequestParam(value = "draft", defaultValue = "false") boolean draft) {

        if (categoryId != null) {
            event.setCategory(eventCategoryService.getEventCategoryById(categoryId));
        } else {
            event.setCategory(null);
        }

        if (!image.isEmpty()) {
            Path imageDirectory = Paths.get("uploads");
            try {
                if (!Files.exists(imageDirectory)) {
                    Files.createDirectories(imageDirectory);
                }
                String uniqueFilename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path fullPath = imageDirectory.resolve(uniqueFilename);
                Files.copy(image.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
                event.setImageUrl("/uploads/" + uniqueFilename);
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

        if (event.getId() == null) {
            // new event: active unless created as draft
            event.setIsActive(!draft);
        } else {
            // editing: always preserve the existing isActive value
            Event existing = eventService.getEventById(event.getId());
            if (existing != null) {
                event.setIsActive(existing.getIsActive());
            }
        }

        eventService.saveEvent(event);
        return draft ? "redirect:/admin/trash" : "redirect:/admin";
    }

    @GetMapping("/event/deactivate/{id}")
    public String deactivateEvent(@PathVariable Integer id) {
        eventService.deactivateEvent(id);
        return "redirect:/admin";
    }

    @GetMapping("/event/reactivate/{id}")
    public String reactivateEvent(@PathVariable Integer id) {
        eventService.reactivateEvent(id);
        return "redirect:/admin/trash";
    }

    @GetMapping("/event/delete/{id}")
    public String deleteEvent(@PathVariable Integer id) {
        eventService.deleteEvent(id);
        return "redirect:/admin/trash";
    }

    @GetMapping("/reportes")
    public String showReports(Model model) {
        model.addAttribute("interestRanking", interestReportService.getEventInterestRanking());
        return "reports";
    }
}