package co.edu.uptc.Ticketeo.reports.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<Integer, String> eventStatusById = events.stream()
            .collect(Collectors.toMap(Event::getId, this::resolveEventStatus));

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
        model.addAttribute("selectedEventStatus", resolveEventStatus(selectedEvent));
        model.addAttribute("selectedEventStatusClass", resolveEventStatusClass(selectedEvent));
        model.addAttribute("eventStatusById", eventStatusById);
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("salesRows", salesRows);
        model.addAttribute("totalTicketsSold", totalTicketsSold);
        model.addAttribute("totalRevenue", totalRevenue);

        return "reports/adminTicketSalesByEventReport";
    }

    private String resolveEventStatus(Event event) {
        if (event == null) {
            return "No disponible";
        }
        if (!Boolean.TRUE.equals(event.getIsActive())) {
            return "Inactivo";
        }
        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            return "Completado";
        }
        return "Activo";
    }

    private String resolveEventStatusClass(Event event) {
        if (event == null) {
            return "is-unknown";
        }

        String status = resolveEventStatus(event);
        if ("Inactivo".equals(status)) {
            return "is-inactive";
        }
        if ("Completado".equals(status)) {
            return "is-completed";
        }
        return "is-active";
    }
}
