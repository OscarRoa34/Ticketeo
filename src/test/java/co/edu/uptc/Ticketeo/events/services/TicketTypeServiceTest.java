package co.edu.uptc.Ticketeo.events.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    @InjectMocks
    private TicketTypeService ticketTypeService;

    @Test
    void deleteTicketType_removesAssignmentsBeforeEntity() {
        // Verifica que al eliminar un tipo de ticket,
        // primero se eliminan sus asignaciones en eventos.
        ticketTypeService.deleteTicketType(5);

        verify(eventTicketTypeRepository).deleteByTicketType_Id(5);
        verify(ticketTypeRepository).deleteById(5);
    }

    @Test
    void saveTicketType_persistsEntity() {
        TicketType ticketType = TicketType.builder().name("VIP").build();

        ticketTypeService.saveTicketType(ticketType);

        verify(ticketTypeRepository).save(ticketType);
    }
}
