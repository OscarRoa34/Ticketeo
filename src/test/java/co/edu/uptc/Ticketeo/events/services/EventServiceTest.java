package co.edu.uptc.Ticketeo.events.services;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.interest.repositories.InterestReportRepository;


@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private InterestReportRepository interestReportRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void getActiveEventsFiltered_withSearchAndCategory_usesCombinedQuery() {
        // Verifica que si se proporciona texto de búsqueda y categoría,
        // el servicio usa la consulta combinada por nombre, categoría y estado activo.
        Page<Event> expected = new PageImpl<>(List.of(Event.builder().id(1).name("Rock Fest").build()));
        when(eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(eq("rock"), eq(3), any(Pageable.class)))
                .thenReturn(expected);

        Page<Event> result = eventService.getActiveEventsFiltered("rock", 3, 0, 6);

        assertEquals(expected, result);
        verify(eventRepository).findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(eq("rock"), eq(3), any(Pageable.class));
    }

    @Test
    void getActiveEventsFiltered_withoutFilters_usesDefaultActiveQuery() {
        // Verifica que si no hay filtros (búsqueda vacía y sin categoría),
        // el servicio usa la consulta por eventos activos sin condiciones adicionales.
        Page<Event> expected = new PageImpl<>(List.of());
        when(eventRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(expected);

        Page<Event> result = eventService.getActiveEventsFiltered("   ", null, 1, 4);

        assertEquals(expected, result);
        verify(eventRepository).findByIsActiveTrue(any(Pageable.class));
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
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        eventService.deactivateEvent(10);

        assertFalse(event.getIsActive());
        verify(eventRepository).save(event);
    }

    @Test
    void deactivateEvent_missingEvent_doesNotSave() {
        // Verifica que si el evento no existe,
        // no se intenta guardar ningún cambio en el repositorio.
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        eventService.deactivateEvent(99);

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteEvent_deletesInterestReportBeforeEvent() {
        // Verifica que al eliminar un evento,
        // primero se eliminan los intereses asociados y luego el evento.
        eventService.deleteEvent(7);

        verify(interestReportRepository).deleteByEventId(7);
        verify(eventRepository).deleteById(7);
    }
}