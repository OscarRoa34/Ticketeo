package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {

    private static final int PAGE_SIZE = 6;

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final TicketTypeService ticketTypeService;

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
        return "events/adminEvents";
    }

    @GetMapping("/completed")
    public String showCompletedEvents(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Integer categoryId,
                                      Model model) {
        Page<Event> eventPage = eventService.getCompletedEventsFiltered(search, categoryId, page, PAGE_SIZE);
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        return "events/adminCompletedEvents";
    }

    @GetMapping("/event/new")
    public String showCreateForm(Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        model.addAttribute("ticketQuantities", Map.<Integer, Integer>of());
        model.addAttribute("draft", false);
        return "events/adminEventForm";
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/admin";
        }
        model.addAttribute("event", event);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        model.addAttribute("ticketQuantities", eventService.getTicketTypeQuantitiesForEvent(id));
        model.addAttribute("draft", !Boolean.TRUE.equals(event.getIsActive()));
        return "events/adminEventForm";
    }

    @PostMapping("/event/save")
    public String saveEvent(@ModelAttribute Event event,
                            @RequestParam("imageFile") MultipartFile image,
                            @RequestParam(value = "category", required = false) Integer categoryId,
                            @RequestParam(value = "ticketTypeIds", required = false) List<Integer> ticketTypeIds,
                            @RequestParam Map<String, String> allParams,
                            @RequestParam(value = "draft", defaultValue = "false") boolean draft) {

        event.setCategory(resolveCategoryForSave(categoryId));
        handleImageUpload(event, image);
        preserveActiveStatus(event);
        event.setIsActive(!draft);

        Map<Integer, Integer> ticketQuantities = extractTicketQuantities(ticketTypeIds, allParams);
        eventService.saveEventWithTicketTypes(event, ticketQuantities);
        return draft ? "redirect:/admin/inactive" : "redirect:/admin";
    }

    private EventCategory resolveCategoryForSave(Integer categoryId) {
        if (categoryId != null) {
            EventCategory selected = eventCategoryService.getEventCategoryById(categoryId);
            if (selected != null) {
                return selected;
            }
        }
        return null;
    }

    @GetMapping("/event/deactivate/{id}")
    public String deactivateEvent(@PathVariable Integer id) {
        eventService.deactivateEvent(id);
        return "redirect:/admin";
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

    private void preserveActiveStatus(Event event) {
        if (event.getId() == null) {
            event.setIsActive(true);
        } else {
            Event existing = eventService.getEventById(event.getId());
            if (existing != null) {
                event.setIsActive(existing.getIsActive());
            }
        }
    }

    private Map<Integer, Integer> extractTicketQuantities(List<Integer> ticketTypeIds, Map<String, String> allParams) {
        Map<Integer, Integer> ticketQuantities = new HashMap<>();
        if (ticketTypeIds == null || ticketTypeIds.isEmpty()) {
            return ticketQuantities;
        }

        for (Integer ticketTypeId : ticketTypeIds) {
            String quantityValue = allParams.get("ticketQuantity_" + ticketTypeId);
            if (quantityValue == null || quantityValue.isBlank()) {
                continue;
            }
            try {
                int quantity = Integer.parseInt(quantityValue.trim());
                if (quantity > 0) {
                    ticketQuantities.put(ticketTypeId, quantity);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ticketQuantities;
    }
}