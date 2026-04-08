package co.edu.uptc.Ticketeo.events.services;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.repositories.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
class TicketTypeServiceTest {

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    @SuppressWarnings("unused")
    private EventTicketTypeRepository eventTicketTypeRepository;

    @Mock
    @SuppressWarnings("unused")
    private EventService eventService;

    @InjectMocks
    private TicketTypeService ticketTypeService;

    @Test
    void saveTicketType_blankName_throwsIllegalArgumentException() {
        TicketType ticketType = new TicketType();
        ticketType.setName("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ticketTypeService.saveTicketType(ticketType));

        assertEquals("El nombre del tipo de ticket es obligatorio.", ex.getMessage());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void saveTicketType_newDuplicateName_throwsIllegalStateException() {
        TicketType ticketType = new TicketType();
        ticketType.setName("  GeNeRaL ");
        when(ticketTypeRepository.existsByNameIgnoreCase("GeNeRaL")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ticketTypeService.saveTicketType(ticketType));

        assertEquals("Ya existe un tipo de ticket con ese nombre.", ex.getMessage());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void saveTicketType_updateDuplicateName_throwsIllegalStateException() {
        TicketType ticketType = new TicketType();
        ticketType.setId(5);
        ticketType.setName("VIP");
        when(ticketTypeRepository.existsByNameIgnoreCaseAndIdNot("VIP", 5)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ticketTypeService.saveTicketType(ticketType));

        assertEquals("Ya existe un tipo de ticket con ese nombre.", ex.getMessage());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void saveTicketType_validName_trimsAndSaves() {
        TicketType ticketType = new TicketType();
        ticketType.setName("  General  ");

        when(ticketTypeRepository.existsByNameIgnoreCase("General")).thenReturn(false);
        when(ticketTypeRepository.save(ticketType)).thenReturn(ticketType);

        TicketType saved = ticketTypeService.saveTicketType(ticketType);

        assertEquals("General", saved.getName());
        verify(ticketTypeRepository).save(ticketType);
    }

    @Test
    void getTicketTypeNamesExcludingId_filtersByIdAndBlankNames() {
        TicketType vip = new TicketType();
        vip.setId(1);
        vip.setName("VIP");

        TicketType blank = new TicketType();
        blank.setId(2);
        blank.setName("   ");

        TicketType general = new TicketType();
        general.setId(3);
        general.setName("General");

        when(ticketTypeRepository.findAll(Sort.by("name").ascending())).thenReturn(List.of(vip, blank, general));

        List<String> names = ticketTypeService.getTicketTypeNamesExcludingId(1);

        assertEquals(List.of("General"), names);
    }
}
