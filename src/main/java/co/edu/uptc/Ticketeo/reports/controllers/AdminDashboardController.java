package co.edu.uptc.Ticketeo.reports.controllers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.uptc.Ticketeo.reports.services.AdminDashboardService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping({"", "/"})
    public String showDashboard(Model model) {
        AdminDashboardService.DashboardMetrics metrics = adminDashboardService.getDashboardMetrics();
        model.addAttribute("totalCompanyRevenueFormatted", formatCurrency(metrics.getTotalCompanyRevenue()));
        model.addAttribute("activeEventsCount", metrics.getActiveEventsCount());
        model.addAttribute("completedEventsCount", metrics.getCompletedEventsCount());
        model.addAttribute("registeredUsersCount", metrics.getRegisteredUsersCount());
        return "reports/adminDashboard";
    }

    private String formatCurrency(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return "$ " + formatter.format(amount);
    }
}
