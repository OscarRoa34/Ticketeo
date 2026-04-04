package co.edu.uptc.Ticketeo.reports.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.reports.models.TicketSalesByTypeRow;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
import co.edu.uptc.Ticketeo.reports.services.TicketSalesReportService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final InterestReportService interestReportService;
    private final TicketSalesReportService ticketSalesReportService;

    @GetMapping({"", "/"})
    public String showReportsMenu() {
        return "reports/adminReportsMenu";
    }

    @GetMapping("/interest")
    public String showInterestReport(Model model) {
        model.addAttribute("interestRanking", interestReportService.getEventInterestRanking());
        return "reports/adminInterestReport";
    }

    @GetMapping("/tickets-sales-report-by-event")
    public String showTicketSalesReportByEvent(
            @RequestParam(value = "eventId", required = false) Integer eventId,
            Model model
    ) {
        List<Event> events = ticketSalesReportService.getEventsForReportSelection();
        Event selectedEvent = ticketSalesReportService.getEventById(eventId);

        List<TicketSalesByTypeRow> salesRows = selectedEvent != null
                ? ticketSalesReportService.getTicketSalesByTypeForEvent(selectedEvent.getId())
                : List.of();

        long totalTicketsSold = salesRows.stream()
                .mapToLong(TicketSalesByTypeRow::getSoldTickets)
                .sum();
        double totalRevenue = salesRows.stream()
                .mapToDouble(TicketSalesByTypeRow::getTotalRevenue)
                .sum();

        model.addAttribute("events", events);
        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("salesRows", salesRows);
        model.addAttribute("totalTicketsSold", totalTicketsSold);
        model.addAttribute("totalRevenue", totalRevenue);

        return "reports/adminTicketSalesByEventReport";
    }
}
