package co.edu.uptc.Ticketeo.purchase.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchaseRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock
    private EventTicketTypeRepository eventTicketTypeRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PurchasedTicketRepository purchasedTicketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private PurchaseService purchaseService;

    @Test
    void processPurchase_validData_returnsCheckoutResult() {
        Event event = Event.builder().id(7).name("Concert").date(LocalDate.now().plusDays(3)).price(70.0).build();
        User user = User.builder().id(1L).username("maria").password("x").role(Role.USER).build();
        TicketType vipType = TicketType.builder().id(1).name("VIP").build();
        EventTicketType assignment = EventTicketType.builder()
                .event(event)
                .ticketType(vipType)
                .availableQuantity(5)
                .ticketPrice(100.0)
                .build();
        Purchase savedPurchase = Purchase.builder()
                .id(99L)
                .user(user)
                .eventId(7)
                .eventName("Concert")
                .eventDate(event.getDate())
                .paymentMethod(PaymentMethod.CARD)
                .totalPaid(100.0)
                .totalTickets(1)
                .build();

        when(eventService.getEventById(7)).thenReturn(event);
        when(eventService.isCompletedEvent(event)).thenReturn(false);
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(user));
        when(eventTicketTypeRepository.findByEvent_Id(7)).thenReturn(List.of(assignment));
        when(eventTicketTypeRepository.saveAll(anyList())).thenReturn(List.of(assignment));
        when(purchaseRepository.save(any(Purchase.class))).thenReturn(savedPurchase);
        when(purchasedTicketRepository.save(any(PurchasedTicket.class))).thenReturn(PurchasedTicket.builder().id(1L).build());

        PurchaseService.PurchaseCheckoutResult result = purchaseService.processPurchase(7, "maria", Map.of(1, 1), "card");

        assertEquals(99L, result.purchaseId());
        assertEquals(1, result.tickets());
        assertEquals(100L, result.total());
        assertEquals("CARD", result.paymentMethod());
        assertEquals(1, result.ticketTypeBreakdown().get("VIP"));
        assertEquals(4, assignment.getAvailableQuantity());
        verify(eventTicketTypeRepository).saveAll(anyList());
        verify(purchaseRepository).save(any(Purchase.class));
        verify(purchasedTicketRepository).save(any(PurchasedTicket.class));
        verify(eventService).recalculateMinimumAvailablePrice(7);
    }

    @Test
    void processPurchase_eventNotFound_throwsIllegalArgumentException() {
        when(eventService.getEventById(7)).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> purchaseService.processPurchase(7, "maria", Map.of(1, 1), "card"));

        assertTrue(exception.getMessage().contains("No se encontro el evento"));
    }
}