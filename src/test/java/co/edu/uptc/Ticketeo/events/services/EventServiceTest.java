package co.edu.uptc.Ticketeo.events.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    @Test
    void getEventsFiltered_recentlyAdded_sortsByRealCreationDateDescending() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, null, 0, 6, "date_desc");

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrue(argThat(pageable ->
                hasOrder(pageable, "createdAt", Sort.Direction.DESC)
                        && hasOrder(pageable, "id", Sort.Direction.DESC)
        ));
    }

    @Test
    void getEventsFiltered_upcomingWithoutFilters_usesOnlyFutureEventsAndNearestDateSort() {
        Page<Event> expected = new PageImpl<>(List.of());
        LocalDate today = LocalDate.now();
        when(eventRepository.findByIsActiveTrueAndDateGreaterThanEqual(eq(today), any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, null, 0, 6, "date_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrueAndDateGreaterThanEqual(eq(today), argThat(pageable ->
                hasOrder(pageable, "date", Sort.Direction.ASC)
                        && hasOrder(pageable, "id", Sort.Direction.ASC)
        ));
        verify(eventRepository, never()).findByIsActiveTrue(any(Pageable.class));
    }

    @Test
    void getEventsFiltered_upcomingWithSearchAndCategory_usesFutureFilteredRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        LocalDate today = LocalDate.now();
        when(eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(
                eq("rock"), eq(2), eq(today), any(Pageable.class)
        )).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered("rock", 2, 0, 6, "date_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(
                eq("rock"), eq(2), eq(today), any(Pageable.class)
        );
    }

    @Test
    void getEventsFiltered_upcomingWithCategoryOnly_usesFutureFilteredCategoryRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        LocalDate today = LocalDate.now();
        when(eventRepository.findByCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(eq(3), eq(today), any(Pageable.class)))
                .thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, 3, 0, 6, "date_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(eq(3), eq(today), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_upcomingWithSearchOnly_usesFutureFilteredSearchRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        LocalDate today = LocalDate.now();
        when(eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrueAndDateGreaterThanEqual(eq("jazz"), eq(today), any(Pageable.class)))
                .thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered("jazz", null, 0, 6, "date_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByNameContainingIgnoreCaseAndIsActiveTrueAndDateGreaterThanEqual(eq("jazz"), eq(today), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_upcomingWithBlankSearch_treatsSearchAsMissing() {
        Page<Event> expected = new PageImpl<>(List.of());
        LocalDate today = LocalDate.now();
        when(eventRepository.findByIsActiveTrueAndDateGreaterThanEqual(eq(today), any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered("   ", null, 0, 6, "date_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrueAndDateGreaterThanEqual(eq(today), any(Pageable.class));
        verify(eventRepository, never()).findByNameContainingIgnoreCaseAndIsActiveTrueAndDateGreaterThanEqual(any(), any(), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_nonUpcomingWithSearchAndCategory_usesStandardRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(eq("rock"), eq(1), any(Pageable.class)))
                .thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered("rock", 1, 0, 6, "price_desc");

        assertEquals(expected, result);
        verify(eventRepository).findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(eq("rock"), eq(1), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_nonUpcomingWithCategoryOnly_usesStandardCategoryRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByCategory_IdAndIsActiveTrue(eq(1), any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, 1, 0, 6, "price_desc");

        assertEquals(expected, result);
        verify(eventRepository).findByCategory_IdAndIsActiveTrue(eq(1), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_nonUpcomingWithSearchOnly_usesStandardSearchRepository() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(eq("pop"), any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered("pop", null, 0, 6, "price_desc");

        assertEquals(expected, result);
        verify(eventRepository).findByNameContainingIgnoreCaseAndIsActiveTrue(eq("pop"), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_priceAsc_appliesPriceOrder() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, null, 0, 6, "price_asc");

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrue(argThat(pageable ->
                hasOrder(pageable, "price", Sort.Direction.ASC)
        ));
    }

    @Test
    void getEventsFiltered_priceDesc_appliesPriceOrder() {
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getEventsFiltered(null, null, 0, 6, "price_desc");

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrue(argThat(pageable ->
                hasOrder(pageable, "price", Sort.Direction.DESC)
        ));
    }

    private boolean hasOrder(Pageable pageable, String property, Sort.Direction direction) {
        Sort.Order order = pageable.getSort().getOrderFor(property);
        return order != null && order.getDirection() == direction;
    }
}