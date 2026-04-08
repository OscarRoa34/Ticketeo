package co.edu.uptc.Ticketeo.events.controllers.publicview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.services.UserService;

@ExtendWith(MockitoExtension.class)
class EventPurchaseControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventTicketTypeRepository eventTicketTypeRepository;

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private UserService userService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EventPurchaseController eventPurchaseController;

    @Test
    void showPurchasePage_profileIncomplete_redirectsToProfile() {
        Event event = Event.builder().id(5).date(LocalDate.now().plusDays(2)).build();
        User user = User.builder().username("juan").build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("juan");
        when(eventService.getEventById(5)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userService.getByUsername("juan")).thenReturn(user);
        when(userService.isProfileComplete(user)).thenReturn(false);

        String view = eventPurchaseController.showPurchasePage(5, model, authentication, redirectAttributes);

        assertEquals("redirect:/user/profile?returnUrl=/event/5/purchase", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Completa tu perfil antes de comprar boletas.");
    }

    @Test
    void processPurchase_profileIncomplete_redirectsToProfile() {
        Event event = Event.builder().id(8).date(LocalDate.now().plusDays(5)).build();
        User user = User.builder().username("ana").build();
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("ana");
        when(eventService.getEventById(8)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userService.getByUsername("ana")).thenReturn(user);
        when(userService.isProfileComplete(user)).thenReturn(false);

        String view = eventPurchaseController.processPurchase(8, Map.of(), "CARD", redirectAttributes, authentication);

        assertEquals("redirect:/user/profile?returnUrl=/event/8/purchase", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Completa tu perfil antes de comprar boletas.");
    }
}

