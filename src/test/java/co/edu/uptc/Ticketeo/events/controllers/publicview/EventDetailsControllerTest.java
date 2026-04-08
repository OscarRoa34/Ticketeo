package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class EventDetailsControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private InterestReportService interestReportService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Model model;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventDetailsController eventDetailsController;

    @Test
    void viewEventDetails_eventNotFound_redirectsToHomeWithError() {
        when(eventService.getEventById(99)).thenReturn(null);

        String view = eventDetailsController.viewEventDetails(99, model, authentication);

        assertEquals("redirect:/?error=notfound", view);
        verify(model, never()).addAttribute(anyString(), any());
    }

    @Test
    void viewEventDetails_authenticatedUserWithInterest_setsModelAttributes() {
        Event event = Event.builder().id(5).name("Concierto").date(LocalDate.now().plusDays(2)).build();
        when(eventService.getEventById(5)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(eventService.hasAvailableTicketsForEvent(5)).thenReturn(true);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("laura");
        when(userRepository.findUserIdByUsername("laura")).thenReturn(Optional.of(55L));
        when(interestReportService.isUserInterested(5, 55L)).thenReturn(true);

        String view = eventDetailsController.viewEventDetails(5, model, authentication);

        assertEquals("events/eventDetails", view);
        verify(model).addAttribute("event", event);
        verify(model).addAttribute("isInterested", true);
        verify(model).addAttribute("isCompletedEvent", false);
        verify(model).addAttribute("hasAvailableTicketsForEvent", true);
    }

    @Test
    void viewEventDetails_authenticatedUserWithoutId_keepsNotInterested() {
        Event event = Event.builder().id(6).name("Festival").date(LocalDate.now().plusDays(4)).build();
        when(eventService.getEventById(6)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(eventService.hasAvailableTicketsForEvent(6)).thenReturn(false);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("maria");
        when(userRepository.findUserIdByUsername("maria")).thenReturn(Optional.empty());

        String view = eventDetailsController.viewEventDetails(6, model, authentication);

        assertEquals("events/eventDetails", view);
        verify(model).addAttribute("isInterested", false);
        verify(interestReportService, never()).isUserInterested(any(), any());
    }

    @Test
    void registerInterestAjax_eventNotFound_returnsUnauthorized() {
        when(eventService.getEventById(17)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(17, authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("Debes iniciar sesión para gestionar tu interés", body.get("message"));
    }

    @Test
    void registerInterestAjax_nullAuthentication_returnsUnauthorized() {
        Event event = Event.builder().id(18).date(LocalDate.now().plusDays(3)).build();
        when(eventService.getEventById(18)).thenReturn(event);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(18, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
    }

    @Test
    void registerInterestAjax_notAuthenticated_returnsUnauthorized() {
        Event event = Event.builder().id(19).date(LocalDate.now().plusDays(3)).build();
        when(eventService.getEventById(19)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(19, authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
    }

    @Test
    void registerInterestAjax_completedEvent_returnsConflictAndDoesNotToggleInterest() {
        Event event = Event.builder().id(7).date(LocalDate.now().minusDays(1)).build();
        when(eventService.getEventById(7)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(eventService.isCompletedEvent(event)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(7, authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("No puedes mostrar interés en un evento completado.", body.get("message"));
        verify(interestReportService, never()).toggleInterest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerInterestAjax_userNotFound_returnsUnauthorized() {
        Event event = Event.builder().id(8).name("Concierto").date(LocalDate.now().plusDays(2)).build();
        when(eventService.getEventById(8)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("laura");
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userRepository.findByUsername("laura")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(8, authentication);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("success"));
        assertEquals("No se pudo identificar el usuario", body.get("message"));
        verify(interestReportService, never()).toggleInterest(any(), any());
    }

    @Test
    void registerInterest_completedEvent_redirectsWithErrorMessage() {
        Event event = Event.builder().id(12).date(LocalDate.now().minusDays(2)).build();
        when(eventService.getEventById(12)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(true);

        String view = eventDetailsController.registerInterest(12, redirectAttributes, authentication);

        assertEquals("redirect:/event/12", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No puedes mostrar interés en un evento completado.");
        verify(interestReportService, never()).toggleInterest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registerInterestAjax_activeEvent_togglesInterestAndReturnsSuccess() {
        Event event = Event.builder().id(4).date(LocalDate.now().plusDays(3)).name("Concierto").build();
        User user = User.builder().id(55L).username("laura").build();

        when(eventService.getEventById(4)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("laura");
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userRepository.findByUsername("laura")).thenReturn(Optional.of(user));
        when(interestReportService.toggleInterest(event, user)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(4, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(true, body.get("interested"));
        assertEquals("¡Genial! Hemos registrado tu interés en Concierto", body.get("message"));
    }

    @Test
    void registerInterestAjax_activeEvent_removesInterestAndReturnsSuccess() {
        Event event = Event.builder().id(10).date(LocalDate.now().plusDays(3)).name("Teatro").build();
        User user = User.builder().id(71L).username("carlos").build();

        when(eventService.getEventById(10)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("carlos");
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(user));
        when(interestReportService.toggleInterest(event, user)).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(10, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(false, body.get("interested"));
        assertEquals("Ya no estás interesado en Teatro", body.get("message"));
    }

    @Test
    void registerInterest_eventNull_redirectsWithoutToggle() {
        when(eventService.getEventById(2)).thenReturn(null);

        String view = eventDetailsController.registerInterest(2, redirectAttributes, authentication);

        assertEquals("redirect:/event/2", view);
        verify(interestReportService, never()).toggleInterest(any(), any());
    }

    @Test
    void registerInterest_activeEvent_notAuthenticated_redirectsWithoutToggle() {
        Event event = Event.builder().id(13).date(LocalDate.now().plusDays(1)).build();
        when(eventService.getEventById(13)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(authentication.isAuthenticated()).thenReturn(false);

        String view = eventDetailsController.registerInterest(13, redirectAttributes, authentication);

        assertEquals("redirect:/event/13", view);
        verify(interestReportService, never()).toggleInterest(any(), any());
    }

    @Test
    void registerInterest_activeEvent_authenticatedUserNotFound_redirectsWithoutToggle() {
        Event event = Event.builder().id(14).date(LocalDate.now().plusDays(1)).build();
        when(eventService.getEventById(14)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("ana");
        when(userRepository.findByUsername("ana")).thenReturn(Optional.empty());

        String view = eventDetailsController.registerInterest(14, redirectAttributes, authentication);

        assertEquals("redirect:/event/14", view);
        verify(interestReportService, never()).toggleInterest(any(), any());
    }

    @Test
    void registerInterest_activeEvent_authenticatedUserFound_togglesInterest() {
        Event event = Event.builder().id(15).date(LocalDate.now().plusDays(1)).build();
        User user = User.builder().id(77L).username("ana").build();
        when(eventService.getEventById(15)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("ana");
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));

        String view = eventDetailsController.registerInterest(15, redirectAttributes, authentication);

        assertEquals("redirect:/event/15", view);
        verify(interestReportService).toggleInterest(event, user);
    }
}
