package co.edu.uptc.Ticketeo.purchase.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.purchase.services.TicketPdfService;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;

@ExtendWith(MockitoExtension.class)
class UserPurchaseControllerTest {

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private TicketPdfService ticketPdfService;

    @Mock
    private EventCategoryService eventCategoryService;

    @Mock
    private EventService eventService;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private UserPurchaseController userPurchaseController;

    @Test
    void showUserPurchases_addsEventStatusMapForActiveAndCompleted() {
        Purchase completedPurchase = Purchase.builder()
                .id(1L)
                .eventName("Evento completado")
                .eventDate(LocalDate.now().minusDays(2))
                .build();

        Purchase activePurchase = Purchase.builder()
                .id(2L)
                .eventName("Evento activo")
                .eventDate(LocalDate.now().plusDays(4))
                .build();

        when(authentication.getName()).thenReturn("usuario");
        when(purchaseService.getUserPurchases("usuario")).thenReturn(List.of(completedPurchase, activePurchase));
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        String view = userPurchaseController.showUserPurchases(authentication, model);

        assertEquals("user/userPurchases", view);
        verify(model).addAttribute("purchases", List.of(completedPurchase, activePurchase));
        ArgumentCaptor<Map> statusCaptor = ArgumentCaptor.forClass(Map.class);
        verify(model).addAttribute(eq("purchaseEventStatus"), statusCaptor.capture());
        Map<?, ?> statusByPurchase = statusCaptor.getValue();
        assertEquals("Completado", statusByPurchase.get(1L));
        assertEquals("Activo", statusByPurchase.get(2L));
    }

    @Test
    void showUserPurchases_usesEventDateFromEventWhenPurchaseDateIsNull() {
        Purchase purchaseWithoutDate = Purchase.builder()
                .id(3L)
                .eventId(100)
                .eventName("Evento sin fecha en compra")
                .build();

        Event completedEvent = Event.builder()
                .id(100)
                .date(LocalDate.now().minusDays(1))
                .build();

        when(authentication.getName()).thenReturn("usuario");
        when(purchaseService.getUserPurchases("usuario")).thenReturn(List.of(purchaseWithoutDate));
        when(eventService.getEventById(100)).thenReturn(completedEvent);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        userPurchaseController.showUserPurchases(authentication, model);

        ArgumentCaptor<Map> statusCaptor = ArgumentCaptor.forClass(Map.class);
        verify(model).addAttribute(eq("purchaseEventStatus"), statusCaptor.capture());
        Map<?, ?> statusByPurchase = statusCaptor.getValue();
        assertEquals("Completado", statusByPurchase.get(3L));
    }

    @Test
    void showUserPurchases_prioritizesCurrentEventDateOverPurchaseSnapshot() {
        Purchase purchaseWithOldSnapshot = Purchase.builder()
                .id(4L)
                .eventId(200)
                .eventDate(LocalDate.now().plusDays(10))
                .eventName("Evento con snapshot desactualizado")
                .build();

        Event currentEvent = Event.builder()
                .id(200)
                .date(LocalDate.now().minusDays(3))
                .build();

        when(authentication.getName()).thenReturn("usuario");
        when(purchaseService.getUserPurchases("usuario")).thenReturn(List.of(purchaseWithOldSnapshot));
        when(eventService.getEventById(200)).thenReturn(currentEvent);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        userPurchaseController.showUserPurchases(authentication, model);

        ArgumentCaptor<Map> statusCaptor = ArgumentCaptor.forClass(Map.class);
        verify(model).addAttribute(eq("purchaseEventStatus"), statusCaptor.capture());
        Map<?, ?> statusByPurchase = statusCaptor.getValue();
        assertEquals("Completado", statusByPurchase.get(4L));
    }
}

