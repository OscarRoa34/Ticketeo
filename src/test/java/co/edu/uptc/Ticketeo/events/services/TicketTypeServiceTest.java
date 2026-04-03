package co.edu.uptc.Ticketeo.events.services;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.repositories.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private EventTicketTypeRepository eventTicketTypeRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private TicketTypeService ticketTypeService;

    @Test
    void deleteTicketType_removesAssignmentsBeforeEntity() {
        // Verifica que al eliminar un tipo de ticket,
        // primero se eliminan sus asignaciones en eventos.
        when(eventTicketTypeRepository.findDistinctEventIdsByTicketTypeId(5)).thenReturn(List.of(10, 11));

        ticketTypeService.deleteTicketType(5);

        verify(eventTicketTypeRepository).findDistinctEventIdsByTicketTypeId(5);
        verify(eventTicketTypeRepository).deleteByTicketType_Id(5);
        verify(ticketTypeRepository).deleteById(5);
        verify(eventService).recalculateMinimumAvailablePrice(10);
        verify(eventService).recalculateMinimumAvailablePrice(11);
    }

    @Test
    void saveTicketType_persistsEntity() {
        TicketType ticketType = TicketType.builder().name("VIP").build();

        ticketTypeService.saveTicketType(ticketType);

        verify(ticketTypeRepository).save(ticketType);
    }
}
