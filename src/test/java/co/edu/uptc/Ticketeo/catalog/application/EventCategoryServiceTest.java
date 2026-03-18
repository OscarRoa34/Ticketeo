package co.edu.uptc.Ticketeo.catalog.application;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.catalog.domain.EventCategory;
import co.edu.uptc.Ticketeo.catalog.infrastructure.repository.EventCategoryRepository;
import co.edu.uptc.Ticketeo.catalog.infrastructure.repository.EventRepository;
import co.edu.uptc.Ticketeo.interest.infrastructure.repository.InterestReportRepository;

@ExtendWith(MockitoExtension.class)
class EventCategoryServiceTest {

    @Mock
    private EventCategoryRepository eventCategoryRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private InterestReportRepository interestReportRepository;

    @InjectMocks
    private EventCategoryService eventCategoryService;

    @Test
    void saveCategory_withoutColor_assignsFirstAvailablePaletteColor() {
        // Verifica que si la categoría no tiene color definido,
        // el servicio asigna el primer color disponible de la paleta.
        EventCategory existingA = EventCategory.builder().id(1).name("Music").color("#E74C3C").build();
        EventCategory existingB = EventCategory.builder().id(2).name("Tech").color("#3498DB").build();
        EventCategory toSave = EventCategory.builder().name("Sports").color(" ").build();

        when(eventCategoryRepository.findAll()).thenReturn(List.of(existingA, existingB));
        when(eventCategoryRepository.save(toSave)).thenReturn(toSave);

        EventCategory saved = eventCategoryService.saveCategory(toSave);

        assertEquals("#2ECC71", saved.getColor());
        verify(eventCategoryRepository).save(toSave);
    }

    @Test
    void saveCategory_withColor_keepsProvidedColor() {
        // Verifica que si la categoría ya tiene un color definido,
        // el servicio lo mantiene sin modificarlo.
        EventCategory toSave = EventCategory.builder().name("Cinema").color("#ABCDEF").build();
        when(eventCategoryRepository.save(toSave)).thenReturn(toSave);

        EventCategory saved = eventCategoryService.saveCategory(toSave);

        assertEquals("#ABCDEF", saved.getColor());
    }

    @Test
    void deleteCategory_removesRelatedDataAndCategory() {
        // Verifica que al eliminar una categoría,
        // primero se eliminan los datos relacionados (intereses),
        // luego se desvincula de eventos, y finalmente se elimina la categoría.
        eventCategoryService.deleteCategory(4);

        verify(interestReportRepository).deleteByEventCategoryId(4);
        verify(eventRepository).detachCategory(4);
        verify(eventCategoryRepository).deleteById(4);
    }
}