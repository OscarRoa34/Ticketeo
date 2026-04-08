package co.edu.uptc.Ticketeo.reports.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;

@ExtendWith(MockitoExtension.class)
class UserInterestControllerTest {

    @Mock
    private InterestReportService interestReportService;

    @InjectMocks
    private UserInterestController userInterestController;

    @Test
    void showUserInterests_loadsUserEventsInModel() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        Event event = new Event();
        event.setId(42);
        when(authentication.getName()).thenReturn("andres");
        when(interestReportService.getUserInterests("andres")).thenReturn(List.of(event));

        ExtendedModelMap model = new ExtendedModelMap();

        String view = userInterestController.showUserInterests(authentication, model);

        assertEquals("reports/userInterests", view);
        assertEquals(List.of(event), model.get("events"));
        verify(interestReportService).getUserInterests("andres");
    }
}

