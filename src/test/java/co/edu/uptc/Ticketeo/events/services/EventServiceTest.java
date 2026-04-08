package co.edu.uptc.Ticketeo.events.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.repositories.TicketTypeRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.reports.repositories.InterestReportRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    @SuppressWarnings("unused")
    private InterestReportRepository interestReportRepository;

    @Mock
    private EventTicketTypeRepository eventTicketTypeRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private PurchasedTicketRepository purchasedTicketRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void saveEventWithTicketTypes_validData_returnsUpdatedEvent() {
        Event eventToSave = Event.builder().name("Concert").build();
        Event savedEvent = Event.builder().id(10).name("Concert").build();
        Event updatedEvent = Event.builder().id(10).name("Concert").price(100.0).build();
        TicketType vipType = TicketType.builder().id(1).name("VIP").build();

        when(eventRepository.save(eventToSave)).thenReturn(savedEvent);
        when(eventTicketTypeRepository.findByEvent_Id(10)).thenReturn(List.of());
        when(ticketTypeRepository.findById(1)).thenReturn(Optional.of(vipType));
        when(eventTicketTypeRepository.save(any(EventTicketType.class)))
                .thenReturn(EventTicketType.builder().event(savedEvent).ticketType(vipType).availableQuantity(3).ticketPrice(100.0).build());
        when(eventTicketTypeRepository.findMinimumAvailableTicketPriceByEventId(10)).thenReturn(100.0);
        when(eventRepository.findById(10)).thenReturn(Optional.of(updatedEvent));

        Event result = eventService.saveEventWithTicketTypes(eventToSave, Map.of(1, 3), Map.of(1, 100.0));

        assertEquals(10, result.getId());
        assertEquals(100.0, result.getPrice());
        verify(eventRepository).save(eventToSave);
        verify(eventTicketTypeRepository).deleteByEvent_Id(10);
        verify(eventTicketTypeRepository).save(any(EventTicketType.class));
    }

    @Test
    void saveEventWithTicketTypes_soldTicketTypeChanged_throwsIllegalArgumentException() {
        Event eventToSave = Event.builder().name("Festival").build();
        Event savedEvent = Event.builder().id(20).name("Festival").build();
        TicketType vipType = TicketType.builder().id(1).name("VIP").build();
        EventTicketType assignment = EventTicketType.builder()
                .event(savedEvent)
                .ticketType(vipType)
                .availableQuantity(10)
                .ticketPrice(50.0)
                .build();

        when(eventRepository.save(eventToSave)).thenReturn(savedEvent);
        when(eventTicketTypeRepository.findByEvent_Id(20)).thenReturn(List.of(assignment));
        when(purchasedTicketRepository.countSoldTicketsByTypeNameForEvent(20))
            .thenReturn(List.<Object[]>of(new Object[] { "VIP", 1L }));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.saveEventWithTicketTypes(eventToSave, Map.of(1, 5), Map.of(1, 50.0)));

        assertTrue(exception.getMessage().contains("No se puede modificar la cantidad"));
    }

    @Test
    void deactivateEvent_validIdWithoutSales_setsEventInactive() {
        Event event = Event.builder().id(5).name("Expo").isActive(true).build();

        when(purchasedTicketRepository.existsByPurchase_EventId(5)).thenReturn(false);
        when(eventRepository.findById(5)).thenReturn(Optional.of(event));

        eventService.deactivateEvent(5);

        assertTrue(Boolean.FALSE.equals(event.getIsActive()));
        verify(eventRepository).save(event);
    }

    @Test
    void deactivateEvent_eventWithSoldTickets_throwsIllegalArgumentException() {
        when(purchasedTicketRepository.existsByPurchase_EventId(8)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.deactivateEvent(8));

        assertTrue(exception.getMessage().contains("No se puede desactivar"));
    }

    @Test
    void saveEventWithTicketTypes_pastDate_throwsIllegalArgumentException() {
        Event eventWithPastDate = Event.builder()
                .name("Evento pasado")
                .date(LocalDate.now().minusDays(1))
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.saveEventWithTicketTypes(eventWithPastDate, Map.of(), Map.of()));

        assertEquals("La fecha del evento no puede ser anterior a hoy.", exception.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void getRandomEvents_excludesCompletedEventsFromCarousel() {
        Event completed = Event.builder().id(1).name("Pasado").date(LocalDate.now().minusDays(1)).isActive(true).build();
        Event active = Event.builder().id(2).name("Futuro").date(LocalDate.now().plusDays(2)).isActive(true).build();
        when(eventRepository.findByIsActiveTrue()).thenReturn(List.of(completed, active));

        List<Event> result = eventService.getRandomEvents(5);

        assertTrue(result.stream().anyMatch(event -> event.getId().equals(2)));
        assertFalse(result.stream().anyMatch(event -> event.getId().equals(1)));
    }
}