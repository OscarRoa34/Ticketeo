package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Base64;
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

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminEventController {

    private static final int PAGE_SIZE = 6;
    private static final int MAX_RESTORED_IMAGE_SIZE = 50 * 1024 * 1024;
    private static final String UPLOADS_DIR = "uploads";
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^\\d]");
    private static final String REDIRECT_ADMIN_EVENTS = "redirect:/admin";
    private static final String REDIRECT_ADMIN_INACTIVE = "redirect:/admin/inactive";
    private static final String REDIRECT_ADMIN_COMPLETED = "redirect:/admin/completed";
    private static final String DEFAULT_OPERATION_ERROR = "No fue posible completar la operacion.";
    private static final String FLASH_SUCCESS_MESSAGE = "successMessage";
    private static final String FLASH_ERROR_MESSAGE = "errorMessage";
    private static final String VIEW_ADMIN_EVENTS = "events/adminEvents";
    private static final String VIEW_ADMIN_COMPLETED_EVENTS = "events/adminCompletedEvents";
    private static final String VIEW_ADMIN_INACTIVE_EVENTS = "events/adminInactiveEvents";
    private static final String VIEW_EVENT_FORM = "events/adminEventForm";
    private static final String MESSAGE_EVENT_DEACTIVATED = "Evento desactivado correctamente.";
    private static final String MESSAGE_EVENT_ACTIVATED = "Evento activado correctamente.";
    private static final String MESSAGE_EVENT_DELETED = "Evento eliminado correctamente.";

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final TicketTypeService ticketTypeService;

    @GetMapping
    public String showActiveEvents(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(required = false) String search,
                                   @RequestParam(required = false) Integer categoryId,
                                   Model model) {
        Page<Event> eventPage = eventService.getActiveEventsFiltered(search, categoryId, page, PAGE_SIZE);
        return renderEventList(model, page, search, categoryId, eventPage, VIEW_ADMIN_EVENTS);
    }

    @GetMapping("/completed")
    public String showCompletedEvents(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Integer categoryId,
                                      Model model) {
        Page<Event> eventPage = eventService.getCompletedEventsFiltered(search, categoryId, page, PAGE_SIZE);
        return renderEventList(model, page, search, categoryId, eventPage, VIEW_ADMIN_COMPLETED_EVENTS);
    }

    @GetMapping("/inactive")
    public String showInactiveEvents(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String search,
                                     @RequestParam(required = false) Integer categoryId,
                                     Model model) {
        Page<Event> eventPage = eventService.getInactiveEventsFiltered(search, categoryId, page, PAGE_SIZE);
        return renderEventList(model, page, search, categoryId, eventPage, VIEW_ADMIN_INACTIVE_EVENTS);
    }

    private String renderEventList(Model model, int page, String search, Integer categoryId,
                                   Page<Event> eventPage, String viewName) {
        populateEventListModel(model, page, search, categoryId, eventPage);
        return viewName;
    }

    private void populateEventListModel(Model model, int page, String search,
                                        Integer categoryId, Page<Event> eventPage) {
        model.addAttribute("events", eventPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventPage.getTotalPages());
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("currentCategory", categoryId);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
    }

    @GetMapping("/event/new")
    public String showCreateForm(@RequestParam(value = "draft", defaultValue = "false") boolean draft,
                                 @RequestParam(value = "selectedTicketTypeId", required = false) Integer selectedTicketTypeId,
                                 @RequestParam(value = "selectedCategoryId", required = false) Integer selectedCategoryId,
                                 Model model) {
        Map<Integer, Integer> ticketQuantities = initializeTicketQuantities(selectedTicketTypeId);
        Event event = createEventWithCategory(selectedCategoryId);
        populateEventFormModel(model, event, ticketQuantities, Map.of(), Map.of(),
                selectedTicketTypeId, selectedCategoryId, draft);
        return VIEW_EVENT_FORM;
    }

    private Event createEventWithCategory(Integer selectedCategoryId) {
        Event event = new Event();
        applySelectedCategory(event, selectedCategoryId);
        return event;
    }

    private Map<Integer, Integer> initializeTicketQuantities(Integer selectedTicketTypeId) {
        Map<Integer, Integer> ticketQuantities = new HashMap<>();
        if (selectedTicketTypeId != null) {
            ticketQuantities.put(selectedTicketTypeId, 1);
        }
        return ticketQuantities;
    }

    private void applySelectedCategory(Event event, Integer selectedCategoryId) {
        EventCategory selectedCategory = resolveCategoryForSave(selectedCategoryId);
        if (selectedCategory != null) {
            event.setCategory(selectedCategory);
        }
    }

    @GetMapping("/event/edit/{id}")
    public String showEditForm(@PathVariable Integer id,
                               @RequestParam(value = "selectedTicketTypeId", required = false) Integer selectedTicketTypeId,
                               @RequestParam(value = "selectedCategoryId", required = false) Integer selectedCategoryId,
                               Model model) {
        Event event = eventService.getEventById(id);
        String redirectPath = resolveEditRedirect(event);
        if (redirectPath != null) {
            return redirectPath;
        }

        applySelectedCategory(event, selectedCategoryId);
        populateEditEventForm(model, event, id, selectedTicketTypeId, selectedCategoryId);
        return VIEW_EVENT_FORM;
    }

    private String resolveEditRedirect(Event event) {
        if (event == null) {
            return REDIRECT_ADMIN_EVENTS;
        }
        boolean isCompleted = Boolean.TRUE.equals(event.getIsActive())
                && event.getDate() != null
                && event.getDate().isBefore(LocalDate.now());
        return isCompleted ? REDIRECT_ADMIN_COMPLETED : null;
    }

    private void populateEditEventForm(Model model, Event event, Integer eventId,
                                       Integer selectedTicketTypeId, Integer selectedCategoryId) {
        Map<Integer, Integer> ticketQuantities = buildTicketQuantitiesForEdit(eventId, selectedTicketTypeId);
        Map<Integer, Double> ticketPrices = buildTicketPricesForEdit(eventId);
        Map<Integer, Boolean> soldTicketTypes = eventService.getSoldTicketTypesForEvent(eventId);
        boolean draft = !Boolean.TRUE.equals(event.getIsActive());
        populateEventFormModel(model, event, ticketQuantities, ticketPrices, soldTicketTypes,
                selectedTicketTypeId, selectedCategoryId, draft);
    }

    private Map<Integer, Integer> buildTicketQuantitiesForEdit(Integer eventId, Integer selectedTicketTypeId) {
        Map<Integer, Integer> ticketQuantities = copyExistingQuantities(eventId);
        if (selectedTicketTypeId != null) {
            ticketQuantities.putIfAbsent(selectedTicketTypeId, 1);
        }
        return ticketQuantities;
    }

    private Map<Integer, Integer> copyExistingQuantities(Integer eventId) {
        Map<Integer, Integer> ticketQuantities = new HashMap<>();
        Map<Integer, Integer> existingQuantities = eventService.getTicketTypeQuantitiesForEvent(eventId);
        if (existingQuantities != null) {
            ticketQuantities.putAll(existingQuantities);
        }
        return ticketQuantities;
    }

    private Map<Integer, Double> buildTicketPricesForEdit(Integer eventId) {
        Map<Integer, Double> existingPrices = eventService.getTicketTypePricesForEvent(eventId);
        Map<Integer, Double> ticketPrices = new HashMap<>();
        if (existingPrices != null) {
            ticketPrices.putAll(existingPrices);
        }
        return ticketPrices;
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
        prepareEventForSave(event, image, categoryId, allParams, draft);
        Map<Integer, Integer> ticketQuantities = extractTicketQuantities(ticketTypeIds, allParams);
        Map<Integer, Double> ticketPrices = extractTicketPrices(ticketTypeIds, allParams);
        return saveEventAndBuildResponse(event, ticketQuantities, ticketPrices, draft,
                isNewEvent, model, redirectAttributes);
    }

    private void prepareEventForSave(Event event, MultipartFile image, Integer categoryId,
                                     Map<String, String> allParams, boolean draft) {
        event.setCategory(resolveCategoryForSave(categoryId));
        handleImageUpload(event, image, allParams.get("restoredImageData"));
        preserveActiveStatus(event);
        event.setIsActive(!draft);
    }

    private String saveEventAndBuildResponse(Event event, Map<Integer, Integer> ticketQuantities,
                                             Map<Integer, Double> ticketPrices, boolean draft,
                                             boolean isNewEvent, Model model,
                                             RedirectAttributes redirectAttributes) {
        try {
            eventService.saveEventWithTicketTypes(event, ticketQuantities, ticketPrices);
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, buildSaveSuccessMessage(isNewEvent, draft));
            return draft ? REDIRECT_ADMIN_INACTIVE : REDIRECT_ADMIN_EVENTS;
        } catch (IllegalArgumentException ex) {
            return buildSaveValidationErrorResponse(model, event, ticketQuantities, ticketPrices,
                    draft, ex.getMessage());
        }
    }

    private String buildSaveValidationErrorResponse(Model model, Event event,
                                                    Map<Integer, Integer> ticketQuantities,
                                                    Map<Integer, Double> ticketPrices,
                                                    boolean draft, String errorMessage) {
        model.addAttribute(FLASH_ERROR_MESSAGE, errorMessage);
        Map<Integer, Boolean> soldTicketTypes = eventService.getSoldTicketTypesForEvent(event.getId());
        populateEventFormModel(model, event, ticketQuantities, ticketPrices, soldTicketTypes,
                null, null, draft);
        return VIEW_EVENT_FORM;
    }

    private void populateEventFormModel(Model model, Event event,
                                        Map<Integer, Integer> ticketQuantities,
                                        Map<Integer, Double> ticketPrices,
                                        Map<Integer, Boolean> soldTicketTypes,
                                        Integer selectedTicketTypeId,
                                        Integer selectedCategoryId,
                                        boolean draft) {
        addEventFormCoreAttributes(model, event, ticketQuantities, ticketPrices, soldTicketTypes);
        addEventFormStateAttributes(model, selectedTicketTypeId, selectedCategoryId, draft);
    }

    private void addEventFormCoreAttributes(Model model, Event event,
                                            Map<Integer, Integer> ticketQuantities,
                                            Map<Integer, Double> ticketPrices,
                                            Map<Integer, Boolean> soldTicketTypes) {
        model.addAttribute("event", event);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("ticketTypes", ticketTypeService.getAllTicketTypes());
        model.addAttribute("ticketQuantities", ticketQuantities);
        model.addAttribute("ticketPrices", ticketPrices);
        model.addAttribute("soldTicketTypes", soldTicketTypes);
    }

    private void addEventFormStateAttributes(Model model, Integer selectedTicketTypeId,
                                             Integer selectedCategoryId, boolean draft) {
        model.addAttribute("newlyCreatedTicketTypeId", selectedTicketTypeId);
        model.addAttribute("newlyCreatedCategoryId", selectedCategoryId);
        model.addAttribute("draft", draft);
    }

    private EventCategory resolveCategoryForSave(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        EventCategory selected = eventCategoryService.getEventCategoryById(categoryId);
        return selected != null ? selected : null;
    }

    @GetMapping("/event/deactivate/{id}")
    public Object deactivateEvent(@PathVariable Integer id,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            eventService.deactivateEvent(id);
            return buildSuccessResponse(request, redirectAttributes,
                    MESSAGE_EVENT_DEACTIVATED, REDIRECT_ADMIN_EVENTS);
        } catch (IllegalArgumentException ex) {
            return buildErrorResponse(request, redirectAttributes, ex.getMessage(), REDIRECT_ADMIN_EVENTS);
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
            return buildSuccessResponse(request, redirectAttributes,
                    MESSAGE_EVENT_ACTIVATED, REDIRECT_ADMIN_INACTIVE);
        } catch (RuntimeException ex) {
            return buildErrorResponse(request, redirectAttributes,
                    DEFAULT_OPERATION_ERROR, REDIRECT_ADMIN_INACTIVE);
        }
    }

    @GetMapping("/event/delete/{id}")
    public Object deleteEvent(@PathVariable Integer id,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        try {
            eventService.deleteEvent(id);
            return buildSuccessResponse(request, redirectAttributes,
                    MESSAGE_EVENT_DELETED, REDIRECT_ADMIN_INACTIVE);
        } catch (RuntimeException ex) {
            return buildErrorResponse(request, redirectAttributes,
                    DEFAULT_OPERATION_ERROR, REDIRECT_ADMIN_INACTIVE);
        }
    }

    private Object buildSuccessResponse(HttpServletRequest request,
                                        RedirectAttributes redirectAttributes,
                                        String successMessage,
                                        String redirectPath) {
        if (isAjaxRequest(request)) {
            return ResponseEntity.ok(successMessage);
        }
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, successMessage);
        return redirectPath;
    }

    private Object buildErrorResponse(HttpServletRequest request,
                                      RedirectAttributes redirectAttributes,
                                      String errorMessage,
                                      String redirectPath) {
        if (isAjaxRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
        }
        redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, errorMessage);
        return redirectPath;
    }

    private String buildSaveSuccessMessage(boolean isNewEvent, boolean draft) {
        if (isNewEvent) {
            return draft ? "Evento creado como inactivo correctamente." : "Evento creado correctamente.";
        }
        return "Evento actualizado correctamente.";
    }

    private void handleImageUpload(Event event, MultipartFile image, String restoredImageData) {
        if (!image.isEmpty()) {
            saveUploadedImage(event, image);
            return;
        }
        if (!tryRestoreImageFromDataUrl(event, restoredImageData) && event.getId() != null) {
            restoreExistingImage(event);
        }
    }

    private void saveUploadedImage(Event event, MultipartFile image) {
        try {
            String filename = saveMultipartImage(image);
            event.setImageUrl(buildImageUrl(filename));
        } catch (IOException e) {
            log.error("Error al guardar imagen del evento", e);
        }
    }

    private String saveMultipartImage(MultipartFile image) throws IOException {
        Path uploadDir = ensureUploadDirectory();
        String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
        Files.copy(image.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    private void restoreExistingImage(Event event) {
        Event existing = eventService.getEventById(event.getId());
        if (existing != null) {
            event.setImageUrl(existing.getImageUrl());
        }
    }

    private boolean tryRestoreImageFromDataUrl(Event event, String restoredImageData) {
        String[] parsedData = parseRestoredImageData(restoredImageData);
        if (parsedData == null) {
            return false;
        }

        byte[] imageBytes = decodeBase64Image(parsedData[1]);
        if (!isValidImageBytes(imageBytes)) {
            return false;
        }
        return saveRestoredImage(event, imageBytes, resolveImageExtension(parsedData[0]));
    }

    private String[] parseRestoredImageData(String restoredImageData) {
        if (restoredImageData == null || restoredImageData.isBlank()) {
            return null;
        }
        int commaIndex = restoredImageData.indexOf(',');
        if (commaIndex <= 0) {
            return null;
        }
        String metadata = restoredImageData.substring(0, commaIndex);
        String encodedData = restoredImageData.substring(commaIndex + 1);
        if (!isValidRestoredMetadata(metadata, encodedData)) {
            return null;
        }
        return new String[] {metadata, encodedData};
    }

    private boolean isValidRestoredMetadata(String metadata, String encodedData) {
        return metadata.startsWith("data:image/")
                && metadata.contains(";base64")
                && !encodedData.isBlank();
    }

    private byte[] decodeBase64Image(String encodedData) {
        try {
            return Base64.getDecoder().decode(encodedData);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValidImageBytes(byte[] imageBytes) {
        return imageBytes != null
                && imageBytes.length > 0
                && imageBytes.length <= MAX_RESTORED_IMAGE_SIZE;
    }

    private String resolveImageExtension(String metadata) {
        if (metadata.startsWith("data:image/png")) {
            return "png";
        }
        if (metadata.startsWith("data:image/webp")) {
            return "webp";
        }
        return "jpg";
    }

    private boolean saveRestoredImage(Event event, byte[] imageBytes, String extension) {
        try {
            String filename = saveImageBytes(imageBytes, "_restored." + extension);
            event.setImageUrl(buildImageUrl(filename));
            return true;
        } catch (IOException e) {
            log.error("Error al restaurar imagen temporal del evento", e);
            return false;
        }
    }

    private String saveImageBytes(byte[] imageBytes, String suffix) throws IOException {
        Path uploadDir = ensureUploadDirectory();
        String filename = UUID.randomUUID() + suffix;
        Files.write(uploadDir.resolve(filename), imageBytes);
        return filename;
    }

    private Path ensureUploadDirectory() throws IOException {
        Path uploadDir = Paths.get(UPLOADS_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        return uploadDir;
    }

    private String buildImageUrl(String filename) {
        return "/uploads/" + filename;
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
            Integer quantity = parseValidQuantity(allParams.get("ticketQuantity_" + ticketTypeId));
            if (quantity != null) {
                ticketQuantities.put(ticketTypeId, quantity);
            }
        }
        return ticketQuantities;
    }

    private Integer parseValidQuantity(String quantityValue) {
        if (quantityValue == null || quantityValue.isBlank()) {
            return null;
        }
        try {
            int quantity = Integer.parseInt(quantityValue.trim());
            return quantity >= 0 ? quantity : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Integer, Double> extractTicketPrices(List<Integer> ticketTypeIds, Map<String, String> allParams) {
        Map<Integer, Double> ticketPrices = new HashMap<>();
        if (ticketTypeIds == null || ticketTypeIds.isEmpty()) {
            return ticketPrices;
        }
        for (Integer ticketTypeId : ticketTypeIds) {
            Double price = parseValidPrice(allParams.get("ticketPrice_" + ticketTypeId));
            if (price != null) {
                ticketPrices.put(ticketTypeId, price);
            }
        }
        return ticketPrices;
    }

    private Double parseValidPrice(String priceValue) {
        if (priceValue == null || priceValue.isBlank()) {
            return null;
        }
        String normalized = NON_DIGIT_PATTERN.matcher(priceValue).replaceAll("");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            double price = Double.parseDouble(normalized);
            return price >= 0 ? price : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}