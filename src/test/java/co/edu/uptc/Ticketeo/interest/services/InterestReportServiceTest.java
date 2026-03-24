package co.edu.uptc.Ticketeo.interest.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.interest.models.InterestReport;
import co.edu.uptc.Ticketeo.interest.repositories.InterestReportRepository;
import co.edu.uptc.Ticketeo.user.models.User;

@ExtendWith(MockitoExtension.class)
class InterestReportServiceTest {

    @Mock
    private InterestReportRepository interestReportRepository;

    @InjectMocks
    private InterestReportService interestReportService;

    @Test
    void toggleInterest_whenAlreadyInterested_removesInterestAndReturnsFalse() {
        // Verifica que si el usuario ya estaba interesado en el evento,
        // el servicio elimina ese interés y retorna false.
        Event event = Event.builder().id(12).build();
        User user = User.builder().id(21L).build();
        when(interestReportRepository.existsByEventIdAndUserId(12, 21L)).thenReturn(true);

        boolean interestedNow = interestReportService.toggleInterest(event, user);

        assertFalse(interestedNow);
        verify(interestReportRepository).deleteByEventIdAndUserId(12, 21L);
    }

    @Test
    void toggleInterest_whenNotInterested_createsInterestAndReturnsTrue() {
        // Verifica que si el usuario no estaba interesado,
        // el servicio crea un nuevo interés y retorna true.
        Event event = Event.builder().id(4).build();
        User user = User.builder().id(9L).build();
        when(interestReportRepository.existsByEventIdAndUserId(4, 9L)).thenReturn(false);

        boolean interestedNow = interestReportService.toggleInterest(event, user);

        assertTrue(interestedNow);
        ArgumentCaptor<InterestReport> reportCaptor = ArgumentCaptor.forClass(InterestReport.class);
        verify(interestReportRepository).save(reportCaptor.capture());

        InterestReport savedReport = reportCaptor.getValue();
        assertSame(event, savedReport.getEvent());
        assertSame(user, savedReport.getUser());
        assertTrue(savedReport.getRegistrationDate() != null);
    }

    @Test
    void isUserInterested_whenUserIdIsNull_returnsFalseWithoutRepositoryCall() {
        // Verifica que si el userId es null,
        // el servicio retorna false sin consultar el repositorio.
        boolean result = interestReportService.isUserInterested(5, null);

        assertFalse(result);
    }

    @Test
    void isUserInterested_whenUserIdPresent_delegatesToRepository() {
        // Verifica que si hay userId,
        // el servicio consulta al repositorio y retorna su resultado.
        when(interestReportRepository.existsByEventIdAndUserId(5, 3L)).thenReturn(true);

        boolean result = interestReportService.isUserInterested(5, 3L);

        assertTrue(result);
        verify(interestReportRepository).existsByEventIdAndUserId(5, 3L);
    }
}