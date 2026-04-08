package co.edu.uptc.Ticketeo.reports.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import co.edu.uptc.Ticketeo.reports.services.AdminDashboardService;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @Test
    void showDashboard_formatsAndExposesMetrics() {
        AdminDashboardService.DashboardMetrics metrics =
                new AdminDashboardService.DashboardMetrics(1234567.89, 8L, 2L, 50L);
        when(adminDashboardService.getDashboardMetrics()).thenReturn(metrics);

        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminDashboardController.showDashboard(model);

        assertEquals("reports/adminDashboard", view);
        assertEquals("$ 1.234.568", model.get("totalCompanyRevenueFormatted"));
        assertEquals(8L, model.get("activeEventsCount"));
        assertEquals(2L, model.get("completedEventsCount"));
        assertEquals(50L, model.get("registeredUsersCount"));
    }
}

