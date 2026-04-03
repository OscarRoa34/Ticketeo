package co.edu.uptc.Ticketeo.events.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
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
import co.edu.uptc.Ticketeo.interest.repositories.InterestReportRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
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
    void getActiveEventsFiltered_withSearchAndCategory_usesCombinedQuery() {
        // Verifica que el listado admin de eventos activos excluye completados
        // y aplica filtro combinado por nombre + categoria.
        Page<Event> expected = new PageImpl<>(List.of(Event.builder().id(1).name("Rock Fest").build()));
        when(eventRepository.findManageableActiveEventsByNameAndCategory(eq("rock"), eq(3), any(java.time.LocalDate.class), any(Pageable.class)))
                .thenReturn(expected);

        Page<Event> result = eventService.getActiveEventsFiltered("rock", 3, 0, 6);

        assertEquals(expected, result);
        verify(eventRepository).findManageableActiveEventsByNameAndCategory(eq("rock"), eq(3), any(java.time.LocalDate.class), any(Pageable.class));
    }

    @Test
    void getActiveEventsFiltered_withoutFilters_usesDefaultActiveQuery() {
        // Verifica que sin filtros se usa la consulta de eventos activos gestionables
        // (no completados por fecha).
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findManageableActiveEvents(any(java.time.LocalDate.class), any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getActiveEventsFiltered("   ", null, 1, 4);

        assertEquals(expected, result);
        verify(eventRepository).findManageableActiveEvents(any(java.time.LocalDate.class), any(Pageable.class));
    }

    @Test
    void getEventsFiltered_priceAsc_appliesExpectedSort() {
        // Verifica que al solicitar ordenamiento por precio ascendente,
        // el Pageable se construye con Sort ASC sobre el campo "price".
        when(eventRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(Page.empty());

        eventService.getEventsFiltered(null, null, 0, 5, "price_asc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findByIsActiveTrue(pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("price");
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void deactivateEvent_existingEvent_setsInactiveAndSaves() {
        // Verifica que si el evento existe,
        // se marca como inactivo y se guarda en el repositorio.
        Event event = Event.builder().id(10).name("Expo").isActive(true).build();
        when(purchasedTicketRepository.existsByPurchase_EventId(10)).thenReturn(false);
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        eventService.deactivateEvent(10);

        assertFalse(event.getIsActive());
        verify(eventRepository).save(event);
    }

    @Test
    void deactivateEvent_missingEvent_doesNotSave() {
        // Verifica que si el evento no existe,
        // no se intenta guardar ningún cambio en el repositorio.
        when(purchasedTicketRepository.existsByPurchase_EventId(99)).thenReturn(false);
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        eventService.deactivateEvent(99);

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deactivateEvent_withSoldTickets_throwsErrorAndDoesNotSave() {
        when(purchasedTicketRepository.existsByPurchase_EventId(12)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> eventService.deactivateEvent(12));

        verify(eventRepository, never()).findById(12);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void recalculateMinimumAvailablePrice_existingEvent_updatesStoredPrice() {
        Event event = Event.builder().id(22).name("Dynamic Price Event").price(50000.0).build();
        when(eventRepository.findById(22)).thenReturn(Optional.of(event));
        when(eventTicketTypeRepository.findMinimumAvailableTicketPriceByEventId(22)).thenReturn(70000.0);

        eventService.recalculateMinimumAvailablePrice(22);

        assertEquals(70000.0, event.getPrice());
        verify(eventRepository).save(event);
    }

    @Test
    void deleteEvent_deletesInterestReportBeforeEvent() {
        // Verifica que al eliminar un evento,
        // primero se eliminan los intereses asociados y luego el evento.
        eventService.deleteEvent(7);

        verify(interestReportRepository).deleteByEventId(7);
        verify(eventTicketTypeRepository).deleteByEvent_Id(7);
        verify(eventRepository).deleteById(7);
    }

    @Test
    void saveEventWithTicketTypes_whenSoldTypeChangesQuantity_throwsError() {
        Event event = Event.builder().id(11).name("Festival").build();
        TicketType vip = TicketType.builder().id(5).name("VIP").build();
        EventTicketType existing = EventTicketType.builder()
                .id(77)
                .event(event)
                .ticketType(vip)
                .availableQuantity(20)
                .ticketPrice(120000.0)
                .build();

        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(eventTicketTypeRepository.findByEvent_Id(11)).thenReturn(List.of(existing));
        when(purchasedTicketRepository.countSoldTicketsByTypeNameForEvent(11))
                .thenReturn(List.<Object[]>of(new Object[]{"VIP", 1L}));

        assertThrows(IllegalArgumentException.class,
                () -> eventService.saveEventWithTicketTypes(event, Map.of(5, 10), Map.of(5, 120000.0)));

        verify(eventTicketTypeRepository, never()).deleteByEvent_Id(11);
    }
}