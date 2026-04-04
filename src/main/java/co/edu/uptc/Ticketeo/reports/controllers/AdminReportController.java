package co.edu.uptc.Ticketeo.reports.controllers;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesReportDto;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
import co.edu.uptc.Ticketeo.reports.services.TicketSalesReportService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String showTicketSalesReportByEvent(@RequestParam(value = "eventId", required = false) Integer eventId, Model model) {
        List<Event> events = ticketSalesReportService.getEventsForReport();

        List<TicketTypeSalesReportDto> ticketSales = List.of();
        Event selectedEvent = null;
        long totalTicketsSold = 0L;
        double totalRevenue = 0.0;

        if (eventId != null) {
            selectedEvent = events.stream()
                    .filter(event -> eventId.equals(event.getId()))
                    .findFirst()
                    .orElse(null);
            ticketSales = ticketSalesReportService.getTicketSalesByEvent(eventId);
            totalTicketsSold = ticketSalesReportService.getTotalTicketsSold(ticketSales);
            totalRevenue = ticketSalesReportService.getTotalRevenue(ticketSales);
        }

        model.addAttribute("events", events);
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("ticketSales", ticketSales);
        model.addAttribute("totalTicketsSold", totalTicketsSold);
        model.addAttribute("totalRevenue", totalRevenue);

        return "reports/adminTicketSalesReportByEvent";
    }
}
