package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.io.IOException;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

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
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^\\d]");
    private static final String REDIRECT_ADMIN_EVENTS = "redirect:/admin";
    private static final String REDIRECT_ADMIN_INACTIVE = "redirect:/admin/inactive";
    private static final String DEFAULT_OPERATION_ERROR = "No fue posible completar la operacion.";

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
        return "events/adminInactiveEvents";
    }

    @GetMapping("/event/new")
    public String showCreateForm(@RequestParam(value = "draft", defaultValue = "false") boolean draft,
                                 Model model) {
        model.addAttribute("event", new Event());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        model.addAttribute("ticketQuantities", Map.<Integer, Integer>of());
        model.addAttribute("ticketPrices", Map.<Integer, Double>of());
        model.addAttribute("soldTicketTypes", Map.<Integer, Boolean>of());
        model.addAttribute("draft", draft);
        return "events/adminEventForm";
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/admin";
        }
        if (Boolean.TRUE.equals(event.getIsActive()) && event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            return "redirect:/admin/completed";
        }
        model.addAttribute("event", event);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        model.addAttribute("ticketQuantities", eventService.getTicketTypeQuantitiesForEvent(id));
        model.addAttribute("ticketPrices", eventService.getTicketTypePricesForEvent(id));
        model.addAttribute("soldTicketTypes", eventService.getSoldTicketTypesForEvent(id));
        model.addAttribute("draft", !Boolean.TRUE.equals(event.getIsActive()));
        return "events/adminEventForm";
    }

    @PostMapping("/event/save")
    public String saveEvent(@ModelAttribute Event event,
                            @RequestParam("imageFile") MultipartFile image,
                            @RequestParam(value = "category", required = false) Integer categoryId,
                            @RequestParam(value = "ticketTypeIds", required = false) List<Integer> ticketTypeIds,
                            @RequestParam Map<String, String> allParams,
                            @RequestParam(value = "draft", defaultValue = "false") boolean draft,
                            Model model,
                            RedirectAttributes redirectAttributes) {

        boolean isNewEvent = event.getId() == null;

        event.setCategory(resolveCategoryForSave(categoryId));
        handleImageUpload(event, image);
        preserveActiveStatus(event);
        event.setIsActive(!draft);

        Map<Integer, Integer> ticketQuantities = extractTicketQuantities(ticketTypeIds, allParams);
        Map<Integer, Double> ticketPrices = extractTicketPrices(ticketTypeIds, allParams);

        try {
            eventService.saveEventWithTicketTypes(event, ticketQuantities, ticketPrices);
            redirectAttributes.addFlashAttribute("successMessage", buildSaveSuccessMessage(isNewEvent, draft));
            return draft ? REDIRECT_ADMIN_INACTIVE : REDIRECT_ADMIN_EVENTS;
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("event", event);
            model.addAttribute("categories", eventCategoryService.getAllCategories());
            model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
            model.addAttribute("ticketQuantities", ticketQuantities);
            model.addAttribute("ticketPrices", ticketPrices);
            model.addAttribute("soldTicketTypes", eventService.getSoldTicketTypesForEvent(event.getId()));
            model.addAttribute("draft", draft);
            return "events/adminEventForm";
        }
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
    public Object deactivateEvent(@PathVariable Integer id,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            eventService.deactivateEvent(id);
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok("Evento desactivado correctamente.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Evento desactivado correctamente.");
            return REDIRECT_ADMIN_EVENTS;
        } catch (IllegalArgumentException ex) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return REDIRECT_ADMIN_EVENTS;
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    @GetMapping("/event/activate/{id}")
    public Object activateEvent(@PathVariable Integer id,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            eventService.reactivateEvent(id);
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok("Evento activado correctamente.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Evento activado correctamente.");
            return REDIRECT_ADMIN_INACTIVE;
        } catch (RuntimeException ex) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(DEFAULT_OPERATION_ERROR);
            }
            redirectAttributes.addFlashAttribute("errorMessage", DEFAULT_OPERATION_ERROR);
            return REDIRECT_ADMIN_INACTIVE;
        }
    }

    @GetMapping("/event/delete/{id}")
    public Object deleteEvent(@PathVariable Integer id,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id);
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok("Evento eliminado correctamente.");
            }
            redirectAttributes.addFlashAttribute("successMessage", "Evento eliminado correctamente.");
            return REDIRECT_ADMIN_INACTIVE;
        } catch (RuntimeException ex) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(DEFAULT_OPERATION_ERROR);
            }
            redirectAttributes.addFlashAttribute("errorMessage", DEFAULT_OPERATION_ERROR);
            return REDIRECT_ADMIN_INACTIVE;
        }
    }

    private String buildSaveSuccessMessage(boolean isNewEvent, boolean draft) {
        if (isNewEvent) {
            return draft ? "Evento creado como inactivo correctamente." : "Evento creado correctamente.";
        }
        return "Evento actualizado correctamente.";
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
                if (quantity >= 0) {
                    ticketQuantities.put(ticketTypeId, quantity);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ticketQuantities;
    }

    private Map<Integer, Double> extractTicketPrices(List<Integer> ticketTypeIds, Map<String, String> allParams) {
        Map<Integer, Double> ticketPrices = new HashMap<>();
        if (ticketTypeIds == null || ticketTypeIds.isEmpty()) {
            return ticketPrices;
        }

        for (Integer ticketTypeId : ticketTypeIds) {
            String priceValue = allParams.get("ticketPrice_" + ticketTypeId);
            if (priceValue == null || priceValue.isBlank()) {
                continue;
            }

            String normalized = NON_DIGIT_PATTERN.matcher(priceValue).replaceAll("");
            if (normalized.isBlank()) {
                continue;
            }

            try {
                double price = Double.parseDouble(normalized);
                if (price >= 0) {
                    ticketPrices.put(ticketTypeId, price);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return ticketPrices;
    }
}