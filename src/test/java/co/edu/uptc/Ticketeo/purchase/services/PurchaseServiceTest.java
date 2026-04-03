package co.edu.uptc.Ticketeo.purchase.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventService;
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
    void processPurchase_updatesStockAndCreatesPurchaseData() {
        Event event = Event.builder().id(1).name("Rock Fest").date(LocalDate.now().plusDays(3)).price(45000.0).build();
        User user = User.builder().id(2L).username("ana").password("pwd").role(Role.USER).build();
        TicketType ticketType = TicketType.builder().id(3).name("VIP").build();
        EventTicketType assignment = EventTicketType.builder()
                .id(1)
                .event(event)
                .ticketType(ticketType)
                .availableQuantity(10)
                .ticketPrice(45000.0)
                .build();

        when(eventService.getEventById(1)).thenReturn(event);
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(eventTicketTypeRepository.findByEvent_Id(1)).thenReturn(List.of(assignment));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase purchase = invocation.getArgument(0);
            purchase.setId(99L);
            return purchase;
        });
        when(purchasedTicketRepository.save(any(PurchasedTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseService.PurchaseCheckoutResult result = purchaseService.processPurchase(1, "ana", Map.of(3, 2), "CARD");

        assertEquals(2, result.tickets());
        assertEquals(90000L, result.total());
        assertEquals("CARD", result.paymentMethod());
        assertEquals(2, result.ticketTypeBreakdown().get("VIP"));
        assertEquals(8, assignment.getAvailableQuantity());
        verify(eventService).recalculateMinimumAvailablePrice(1);
    }

    @Test
    void processPurchase_withoutTicketSelection_throwsError() {
        Event event = Event.builder().id(1).name("Rock Fest").price(45000.0).build();
        User user = User.builder().id(2L).username("ana").password("pwd").role(Role.USER).build();

        when(eventService.getEventById(1)).thenReturn(event);
        when(userRepository.findByUsername("ana")).thenReturn(Optional.of(user));
        when(eventTicketTypeRepository.findByEvent_Id(1)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> purchaseService.processPurchase(1, "ana", Map.of(), "CARD"));
    }
}

