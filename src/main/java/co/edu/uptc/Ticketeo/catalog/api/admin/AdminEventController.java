package co.edu.uptc.Ticketeo.catalog.api.admin;

import co.edu.uptc.Ticketeo.catalog.application.EventCategoryService;
import co.edu.uptc.Ticketeo.catalog.application.EventService;
import co.edu.uptc.Ticketeo.catalog.domain.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {

    private static final int PAGE_SIZE = 6;

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;

    @GetMapping
    public String showActiveEvents(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) Integer categoryId,
                                   Model model) {
        Page<Event> eventPage = eventService.getActiveEventsFiltered(search, categoryId, page, PAGE_SIZE);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        return "adminEvents";
    }

    @GetMapping("/inactive")
    public String showInactiveEvents(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) Integer categoryId,
                                     Model model) {
        Page<Event> eventPage = eventService.getInactiveEventsFiltered(search, categoryId, page, PAGE_SIZE);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        return "adminInactiveEvents";
    }

    @GetMapping("/event/new")
    public String showCreateForm(@RequestParam(defaultValue = "false") boolean draft, Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("draft", draft);
        return "adminEventForm";
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               @RequestParam(defaultValue = "false") boolean fromTrash,
                               Model model) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/admin";
        }
        model.addAttribute("event", event);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("draft", fromTrash);
        return "adminEventForm";
    }

    @PostMapping("/event/save")
    public String saveEvent(@ModelAttribute Event event,
                            @RequestParam("imageFile") MultipartFile image,
                            @RequestParam(value = "category", required = false) Integer categoryId,
                            @RequestParam(value = "draft", defaultValue = "false") boolean draft) {

        event.setCategory(categoryId != null ? eventCategoryService.getEventCategoryById(categoryId) : null);
        handleImageUpload(event, image);
        preserveActiveStatus(event, draft);

        eventService.saveEvent(event);
        return draft ? "redirect:/admin/inactive" : "redirect:/admin";
    }

    @GetMapping("/event/deactivate/{id}")
    public String deactivateEvent(@PathVariable Integer id) {
        eventService.deactivateEvent(id);
        return "redirect:/admin";
    }

    @GetMapping("/event/activate/{id}")
    public String activateEvent(@PathVariable Integer id) {
        eventService.reactivateEvent(id);
        return "redirect:/admin/inactive";
    }

    @GetMapping("/event/delete/{id}")
    public String deleteEvent(@PathVariable Integer id) {
        eventService.deleteEvent(id);
        return "redirect:/admin/inactive";
    }

    private void handleImageUpload(Event event, MultipartFile image) {
        if (!image.isEmpty()) {
            Path uploadDir = Paths.get("uploads");
            try {
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }
                String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Files.copy(image.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
                event.setImageUrl("/uploads/" + filename);
            } catch (IOException e) {
                log.error("Error al guardar imagen del evento", e);
            }
        } else if (event.getId() != null) {
            Event existing = eventService.getEventById(event.getId());
            if (existing != null) {
                event.setImageUrl(existing.getImageUrl());
            }
        }
    }

    private void preserveActiveStatus(Event event, boolean draft) {
        if (event.getId() == null) {
            event.setIsActive(!draft);
        } else {
            Event existing = eventService.getEventById(event.getId());
            if (existing != null) {
                event.setIsActive(existing.getIsActive());
            }
        }
    }
}