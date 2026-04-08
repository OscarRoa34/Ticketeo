package co.edu.uptc.Ticketeo.events.controllers.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AdminEventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventCategoryService eventCategoryService;

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private MultipartFile image;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AdminEventController adminEventController;

    @Test
    void showActiveEvents_loadsPageAndFiltersInModel() {
        Page<Event> eventPage = new PageImpl<>(List.of(new Event()));
        when(eventService.getActiveEventsFiltered("rock", 2, 1, 6)).thenReturn(eventPage);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        String view = adminEventController.showActiveEvents(1, "rock", 2, model);

        assertEquals("events/adminEvents", view);
        verify(model).addAttribute("events", eventPage.getContent());
        verify(model).addAttribute("currentPage", 1);
        verify(model).addAttribute("totalPages", eventPage.getTotalPages());
        verify(model).addAttribute("search", "rock");
        verify(model).addAttribute("currentCategory", 2);
    }

    @Test
    void showCompletedEvents_loadsPageAndFiltersInModel() {
        Page<Event> eventPage = new PageImpl<>(List.of(new Event()));
        when(eventService.getCompletedEventsFiltered("teatro", 3, 0, 6)).thenReturn(eventPage);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        String view = adminEventController.showCompletedEvents(0, "teatro", 3, model);

        assertEquals("events/adminCompletedEvents", view);
        verify(model).addAttribute("events", eventPage.getContent());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", eventPage.getTotalPages());
        verify(model).addAttribute("search", "teatro");
        verify(model).addAttribute("currentCategory", 3);
    }

    @Test
    void showInactiveEvents_loadsPageAndFiltersInModel() {
        Page<Event> eventPage = new PageImpl<>(List.of(new Event()));
        when(eventService.getInactiveEventsFiltered("feria", null, 2, 6)).thenReturn(eventPage);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        String view = adminEventController.showInactiveEvents(2, "feria", null, model);

        assertEquals("events/adminInactiveEvents", view);
        verify(model).addAttribute("events", eventPage.getContent());
        verify(model).addAttribute("currentPage", 2);
        verify(model).addAttribute("totalPages", eventPage.getTotalPages());
        verify(model).addAttribute("search", "feria");
        verify(model).addAttribute("currentCategory", null);
    }

    @Test
    void saveEvent_success_redirectsAndAddsSuccessFlash() {
        Event event = new Event();
        when(image.isEmpty()).thenReturn(true);
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap())).thenReturn(event);

        String view = adminEventController.saveEvent(event, image, null, null, Map.of(), false, model, redirectAttributes);

        assertEquals("redirect:/admin", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento creado correctamente.");
    }

    @Test
    void saveEvent_draftSuccess_redirectsToInactiveAndAddsDraftFlash() {
        Event event = new Event();
        when(image.isEmpty()).thenReturn(true);
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap())).thenReturn(event);

        String view = adminEventController.saveEvent(event, image, null, null, Map.of(), true, model, redirectAttributes);

        assertEquals("redirect:/admin/inactive", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento creado como inactivo correctamente.");
    }

    @Test
    void saveEvent_editSuccess_redirectsAndAddsUpdateFlash() {
        Event event = new Event();
        event.setId(88);
        when(image.isEmpty()).thenReturn(true);
        when(eventService.getEventById(88)).thenReturn(Event.builder().id(88).isActive(true).build());
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap())).thenReturn(event);

        String view = adminEventController.saveEvent(event, image, null, null, Map.of(), false, model, redirectAttributes);

        assertEquals("redirect:/admin", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento actualizado correctamente.");
    }

    @Test
    void saveEvent_validationError_returnsFormWithErrorMessage() {
        Event event = new Event();
        when(image.isEmpty()).thenReturn(true);
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap()))
                .thenThrow(new IllegalArgumentException("Dato invalido"));
        when(eventService.getSoldTicketTypesForEvent(null)).thenReturn(Map.of());
        when(eventCategoryService.getAllCategories()).thenReturn(java.util.List.of());
        when(ticketTypeService.getAllTicketTypes()).thenReturn(java.util.List.of());

        String view = adminEventController.saveEvent(event, image, null, null, Map.of(), false, model, redirectAttributes);

        assertEquals("events/adminEventForm", view);
        verify(model).addAttribute("errorMessage", "Dato invalido");
    }

    @Test
    void deactivateEvent_ajaxError_returnsConflictWithBody() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        doThrow(new IllegalArgumentException("No se puede desactivar")).when(eventService).deactivateEvent(4);

        Object result = adminEventController.deactivateEvent(4, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No se puede desactivar", response.getBody());
    }

    @Test
    void deactivateEvent_ajaxSuccess_returnsOkWithMessage() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        Object result = adminEventController.deactivateEvent(4, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Evento desactivado correctamente.", response.getBody());
    }

    @Test
    void deactivateEvent_nonAjaxSuccess_redirectsAndAddsSuccessFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);

        Object result = adminEventController.deactivateEvent(4, request, redirectAttributes);

        assertEquals("redirect:/admin", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento desactivado correctamente.");
    }

    @Test
    void deactivateEvent_nonAjaxError_redirectsAndAddsSpecificErrorFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        doThrow(new IllegalArgumentException("Evento con ventas")).when(eventService).deactivateEvent(4);

        Object result = adminEventController.deactivateEvent(4, request, redirectAttributes);

        assertEquals("redirect:/admin", result);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Evento con ventas");
    }

    @Test
    void activateEvent_ajaxSuccess_returnsOk() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        Object result = adminEventController.activateEvent(2, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Evento activado correctamente.", response.getBody());
    }

    @Test
    void activateEvent_nonAjaxSuccess_redirectsAndAddsSuccessFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);

        Object result = adminEventController.activateEvent(2, request, redirectAttributes);

        assertEquals("redirect:/admin/inactive", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento activado correctamente.");
    }

    @Test
    void activateEvent_ajaxFailure_returnsConflictWithDefaultMessage() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        doThrow(new RuntimeException("boom")).when(eventService).reactivateEvent(2);

        Object result = adminEventController.activateEvent(2, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No fue posible completar la operacion.", response.getBody());
    }

    @Test
    void activateEvent_nonAjaxFailure_redirectsAndAddsDefaultErrorFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(eventService).reactivateEvent(2);

        Object result = adminEventController.activateEvent(2, request, redirectAttributes);

        assertEquals("redirect:/admin/inactive", result);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No fue posible completar la operacion.");
    }

    @Test
    void deleteEvent_ajaxFailure_returnsConflictWithDefaultMessage() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");
        doThrow(new RuntimeException("Boom")).when(eventService).deleteEvent(9);

        Object result = adminEventController.deleteEvent(9, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No fue posible completar la operacion.", response.getBody());
    }

    @Test
    void deleteEvent_ajaxSuccess_returnsOkWithMessage() {
        when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

        Object result = adminEventController.deleteEvent(9, request, redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Evento eliminado correctamente.", response.getBody());
    }

    @Test
    void deleteEvent_nonAjaxSuccess_redirectsAndAddsSuccessFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);

        Object result = adminEventController.deleteEvent(9, request, redirectAttributes);

        assertEquals("redirect:/admin/inactive", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento eliminado correctamente.");
    }

    @Test
    void deleteEvent_nonAjaxFailure_redirectsAndAddsDefaultErrorFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);
        doThrow(new RuntimeException("Boom")).when(eventService).deleteEvent(9);

        Object result = adminEventController.deleteEvent(9, request, redirectAttributes);

        assertEquals("redirect:/admin/inactive", result);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No fue posible completar la operacion.");
    }

    @Test
    void showCreateForm_defaultBuildsReturnPathAndEmptyTicketMaps() {
        ExtendedModelMap localModel = new ExtendedModelMap();
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());

        String view = adminEventController.showCreateForm(false, null, null, localModel);

        assertEquals("events/adminEventForm", view);
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> ticketQuantities = (Map<Integer, Integer>) localModel.get("ticketQuantities");
        assertNotNull(ticketQuantities);
        assertEquals(0, ticketQuantities.size());
    }

    @Test
    void saveEvent_withTicketTypeParams_parsesAndForwardsMaps() {
        Event event = new Event();
        EventCategory category = new EventCategory();
        category.setId(3);
        when(image.isEmpty()).thenReturn(true);
        when(eventCategoryService.getEventCategoryById(3)).thenReturn(category);
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap())).thenReturn(event);

        String view = adminEventController.saveEvent(
                event,
                image,
                3,
                List.of(10, 11),
                Map.of(
                        "ticketQuantity_10", "4",
                        "ticketQuantity_11", "x",
                        "ticketPrice_10", "$12.500",
                        "ticketPrice_11", ""
                ),
                false,
                model,
                redirectAttributes
        );

        assertEquals("redirect:/admin", view);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Integer>> quantitiesCaptor = (ArgumentCaptor<Map<Integer, Integer>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Double>> pricesCaptor = (ArgumentCaptor<Map<Integer, Double>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Map.class);

        verify(eventService).saveEventWithTicketTypes(eventCaptor.capture(), quantitiesCaptor.capture(), pricesCaptor.capture());
        assertEquals(category, eventCaptor.getValue().getCategory());
        assertEquals(4, quantitiesCaptor.getValue().get(10));
        assertEquals(12500d, pricesCaptor.getValue().get(10));
    }

    @Test
    void showEditForm_eventNotFound_redirectsToAdmin() {
        when(eventService.getEventById(99)).thenReturn(null);

        String view = adminEventController.showEditForm(99, null, null, model);

        assertEquals("redirect:/admin", view);
    }

    @Test
    void showEditForm_completedEvent_redirectsToCompleted() {
        Event completed = new Event();
        completed.setId(1);
        completed.setIsActive(true);
        completed.setDate(LocalDate.now().minusDays(1));
        when(eventService.getEventById(1)).thenReturn(completed);

        String view = adminEventController.showEditForm(1, null, null, model);

        assertEquals("redirect:/admin/completed", view);
    }

    @Test
    void showEditForm_activeEvent_loadsFormModel() {
        Event active = new Event();
        active.setId(2);
        active.setIsActive(true);
        active.setDate(LocalDate.now().plusDays(5));
        active.setCategory(new EventCategory());
        when(eventService.getEventById(2)).thenReturn(active);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());
        when(eventService.getTicketTypeQuantitiesForEvent(2)).thenReturn(Map.of());
        when(eventService.getTicketTypePricesForEvent(2)).thenReturn(Map.of());
        when(eventService.getSoldTicketTypesForEvent(2)).thenReturn(Map.of());

        String view = adminEventController.showEditForm(2, null, null, model);

        assertEquals("events/adminEventForm", view);
        verify(model).addAttribute("event", active);
        verify(model).addAttribute("draft", false);
    }

    @Test
    void showCreateForm_withSelectedTicketType_marksItAsPreselected() {
        ExtendedModelMap localModel = new ExtendedModelMap();
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());

        String view = adminEventController.showCreateForm(true, 7, null, localModel);

        assertEquals("events/adminEventForm", view);
        assertEquals(7, localModel.get("newlyCreatedTicketTypeId"));
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> ticketQuantities = (Map<Integer, Integer>) localModel.get("ticketQuantities");
        assertNotNull(ticketQuantities);
        assertEquals(1, ticketQuantities.get(7));
    }

    @Test
    void showEditForm_withSelectedTicketType_addsDefaultQuantityWhenMissing() {
        ExtendedModelMap localModel = new ExtendedModelMap();
        Event active = new Event();
        active.setId(3);
        active.setIsActive(true);
        active.setDate(LocalDate.now().plusDays(7));

        when(eventService.getEventById(3)).thenReturn(active);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());
        when(eventService.getTicketTypeQuantitiesForEvent(3)).thenReturn(Map.of());
        when(eventService.getTicketTypePricesForEvent(3)).thenReturn(Map.of());
        when(eventService.getSoldTicketTypesForEvent(3)).thenReturn(Map.of());

        String view = adminEventController.showEditForm(3, 12, null, localModel);

        assertEquals("events/adminEventForm", view);
        assertEquals(12, localModel.get("newlyCreatedTicketTypeId"));
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> ticketQuantities = (Map<Integer, Integer>) localModel.get("ticketQuantities");
        assertNotNull(ticketQuantities);
        assertEquals(1, ticketQuantities.get(12));
    }

    @Test
    void showCreateForm_withSelectedCategory_preselectsItInEvent() {
        ExtendedModelMap localModel = new ExtendedModelMap();
        EventCategory category = new EventCategory();
        category.setId(6);
        category.setName("Música");

        when(eventCategoryService.getEventCategoryById(6)).thenReturn(category);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of(category));
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());

        String view = adminEventController.showCreateForm(false, null, 6, localModel);

        assertEquals("events/adminEventForm", view);
        Event modelEvent = (Event) localModel.get("event");
        assertNotNull(modelEvent);
        assertNotNull(modelEvent.getCategory());
        assertEquals(6, modelEvent.getCategory().getId());
        assertEquals(6, localModel.get("newlyCreatedCategoryId"));
    }

    @Test
    void showEditForm_withSelectedCategory_overridesCurrentCategory() {
        ExtendedModelMap localModel = new ExtendedModelMap();
        Event active = new Event();
        active.setId(4);
        active.setIsActive(true);
        active.setDate(LocalDate.now().plusDays(4));
        EventCategory existingCategory = new EventCategory();
        existingCategory.setId(1);
        active.setCategory(existingCategory);

        EventCategory newCategory = new EventCategory();
        newCategory.setId(9);

        when(eventService.getEventById(4)).thenReturn(active);
        when(eventCategoryService.getEventCategoryById(9)).thenReturn(newCategory);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of(existingCategory, newCategory));
        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of());
        when(eventService.getTicketTypeQuantitiesForEvent(4)).thenReturn(Map.of());
        when(eventService.getTicketTypePricesForEvent(4)).thenReturn(Map.of());
        when(eventService.getSoldTicketTypesForEvent(4)).thenReturn(Map.of());

        String view = adminEventController.showEditForm(4, null, 9, localModel);

        assertEquals("events/adminEventForm", view);
        Event modelEvent = (Event) localModel.get("event");
        assertNotNull(modelEvent);
        assertNotNull(modelEvent.getCategory());
        assertEquals(9, modelEvent.getCategory().getId());
        assertEquals(9, localModel.get("newlyCreatedCategoryId"));
    }
}

