package co.edu.uptc.Ticketeo.reports.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchaseRepository;
import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void getDashboardMetrics_whenRevenueExists_returnsAllValues() {
        when(purchaseRepository.getTotalCompanyRevenue()).thenReturn(987654.0);
        when(eventRepository.countManageableActiveEvents(org.mockito.ArgumentMatchers.any())).thenReturn(12L);
        when(eventRepository.countCompletedEvents(org.mockito.ArgumentMatchers.any())).thenReturn(4L);
        when(userRepository.countByRole(Role.USER)).thenReturn(50L);

        AdminDashboardService.DashboardMetrics metrics = adminDashboardService.getDashboardMetrics();

        assertEquals(987654.0, metrics.getTotalCompanyRevenue());
        assertEquals(12L, metrics.getActiveEventsCount());
        assertEquals(4L, metrics.getCompletedEventsCount());
        assertEquals(50L, metrics.getRegisteredUsersCount());
    }

    @Test
    void getDashboardMetrics_whenRevenueIsNull_usesZero() {
        when(purchaseRepository.getTotalCompanyRevenue()).thenReturn(null);
        when(eventRepository.countManageableActiveEvents(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(eventRepository.countCompletedEvents(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(userRepository.countByRole(Role.USER)).thenReturn(1L);

        AdminDashboardService.DashboardMetrics metrics = adminDashboardService.getDashboardMetrics();

        assertEquals(0.0, metrics.getTotalCompanyRevenue());
        assertEquals(0L, metrics.getActiveEventsCount());
        assertEquals(0L, metrics.getCompletedEventsCount());
        assertEquals(1L, metrics.getRegisteredUsersCount());
    }
}

