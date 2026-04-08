package co.edu.uptc.Ticketeo.events.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.repositories.EventCategoryRepository;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.reports.repositories.InterestReportRepository;

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
    void deleteCategory_whenHasAssociatedEvents_throwsExceptionAndDoesNotDelete() {
        when(eventRepository.existsByCategory_Id(10)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> eventCategoryService.deleteCategory(10));

        assertTrue(exception.getMessage().contains("No puedes eliminar una categoria"));
        verify(interestReportRepository, never()).deleteByEventCategoryId(10);
        verify(eventCategoryRepository, never()).deleteById(10);
    }

    @Test
    void deleteCategory_whenNoAssociatedEvents_deletesReportsAndCategory() {
        when(eventRepository.existsByCategory_Id(11)).thenReturn(false);

        eventCategoryService.deleteCategory(11);

        verify(interestReportRepository).deleteByEventCategoryId(11);
        verify(eventCategoryRepository).deleteById(11);
    }
}

