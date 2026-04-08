package co.edu.uptc.Ticketeo.reports.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.reports.models.EventInterestDto;
import co.edu.uptc.Ticketeo.reports.models.InterestReport;
import co.edu.uptc.Ticketeo.reports.repositories.InterestReportRepository;
import co.edu.uptc.Ticketeo.user.models.User;

@ExtendWith(MockitoExtension.class)
class InterestReportServiceTest {

    @Mock
    private InterestReportRepository interestReportRepository;

    @InjectMocks
    private InterestReportService interestReportService;

    @Test
    void toggleInterest_whenAlreadyInterested_removesInterest() {
        Event event = new Event();
        event.setId(10);
        User user = new User();
        user.setId(5L);
        when(interestReportRepository.existsByEventIdAndUserId(10, 5L)).thenReturn(true);

        boolean result = interestReportService.toggleInterest(event, user);

        assertFalse(result);
        verify(interestReportRepository).deleteByEventIdAndUserId(10, 5L);
        verify(interestReportRepository, never()).save(any(InterestReport.class));
    }

    @Test
    void toggleInterest_whenNotInterested_createsInterest() {
        Event event = new Event();
        event.setId(11);
        User user = new User();
        user.setId(6L);
        when(interestReportRepository.existsByEventIdAndUserId(11, 6L)).thenReturn(false);

        boolean result = interestReportService.toggleInterest(event, user);

        assertTrue(result);
        verify(interestReportRepository).save(any(InterestReport.class));
    }

    @Test
    void isUserInterested_whenUserIdNull_returnsFalse() {
        assertFalse(interestReportService.isUserInterested(1, null));
        verify(interestReportRepository, never()).existsByEventIdAndUserId(any(), any());
    }

    @Test
    void isUserInterested_whenUserIdPresent_queriesRepository() {
        when(interestReportRepository.existsByEventIdAndUserId(1, 9L)).thenReturn(true);

        boolean result = interestReportService.isUserInterested(1, 9L);

        assertTrue(result);
    }

    @Test
    void getEventInterestRanking_delegatesToRepository() {
        Event event = new Event();
        event.setId(12);
        EventInterestDto dto = new EventInterestDto() {
            @Override
            public Event getEvent() {
                return event;
            }

            @Override
            public Long getTotalInterests() {
                return 2L;
            }
        };
        when(interestReportRepository.findEventInterestRanking()).thenReturn(List.of(dto));

        List<EventInterestDto> ranking = interestReportService.getEventInterestRanking();

        assertEquals(1, ranking.size());
        assertEquals(2L, ranking.get(0).getTotalInterests());
    }

    @Test
    void getUserInterests_mapsReportsToEvents() {
        Event event = new Event();
        event.setId(20);
        InterestReport report = new InterestReport();
        report.setEvent(event);
        when(interestReportRepository.findByUserUsername("maria")).thenReturn(List.of(report));

        List<Event> events = interestReportService.getUserInterests("maria");

        assertEquals(1, events.size());
        assertEquals(20, events.get(0).getId());
    }
}

