package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private Authentication authentication;

    @InjectMocks
    private EventDetailsController eventDetailsController;

    @Test
    void registerInterestAjax_completedEvent_returnsConflictAndDoesNotToggleInterest() {
        Event event = Event.builder().id(7).date(LocalDate.now().minusDays(1)).build();
        when(eventService.getEventById(7)).thenReturn(event);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(eventService.isCompletedEvent(event)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = eventDetailsController.registerInterestAjax(7, authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        Map<?, ?> body = assertInstanceOf(Map.class, response.getBody());
        assertFalse((Boolean) body.get("success"));
        assertEquals("No puedes mostrar interés en un evento completado.", body.get("message"));
        verify(interestReportService, never()).toggleInterest(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
    }
}
