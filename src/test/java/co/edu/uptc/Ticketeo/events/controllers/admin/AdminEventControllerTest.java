package co.edu.uptc.Ticketeo.events.controllers.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
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
    void saveEvent_success_redirectsAndAddsSuccessFlash() {
        Event event = new Event();
        when(image.isEmpty()).thenReturn(true);
        when(eventService.saveEventWithTicketTypes(any(Event.class), anyMap(), anyMap())).thenReturn(event);

        String view = adminEventController.saveEvent(event, image, null, null, Map.of(), false, model, redirectAttributes);

        assertEquals("redirect:/admin", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento creado correctamente.");
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
    void activateEvent_nonAjaxSuccess_redirectsAndAddsSuccessFlash() {
        when(request.getHeader("X-Requested-With")).thenReturn(null);

        Object result = adminEventController.activateEvent(2, request, redirectAttributes);

        assertEquals("redirect:/admin/inactive", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Evento activado correctamente.");
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
}

