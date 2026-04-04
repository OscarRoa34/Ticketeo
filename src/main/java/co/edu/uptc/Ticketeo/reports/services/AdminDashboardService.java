package co.edu.uptc.Ticketeo.reports.services;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchaseRepository;
import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final PurchaseRepository purchaseRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public DashboardMetrics getDashboardMetrics() {
        LocalDate today = LocalDate.now();
        Double totalCompanyRevenue = purchaseRepository.getTotalCompanyRevenue();

        return new DashboardMetrics(
                totalCompanyRevenue != null ? totalCompanyRevenue : 0.0,
                eventRepository.countManageableActiveEvents(today),
                eventRepository.countCompletedEvents(today),
                userRepository.countByRole(Role.USER));
    }

    public static final class DashboardMetrics {

        private final double totalCompanyRevenue;
        private final long activeEventsCount;
        private final long completedEventsCount;
        private final long registeredUsersCount;

        public DashboardMetrics(double totalCompanyRevenue,
                                long activeEventsCount,
                                long completedEventsCount,
                                long registeredUsersCount) {
            this.totalCompanyRevenue = totalCompanyRevenue;
            this.activeEventsCount = activeEventsCount;
            this.completedEventsCount = completedEventsCount;
            this.registeredUsersCount = registeredUsersCount;
        }

        public double getTotalCompanyRevenue() {
            return totalCompanyRevenue;
        }

        public long getActiveEventsCount() {
            return activeEventsCount;
        }

        public long getCompletedEventsCount() {
            return completedEventsCount;
        }

        public long getRegisteredUsersCount() {
            return registeredUsersCount;
        }
    }
}
