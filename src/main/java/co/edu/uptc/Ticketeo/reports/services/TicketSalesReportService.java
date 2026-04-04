package co.edu.uptc.Ticketeo.reports.services;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesReportDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSalesReportService {

    private final EventRepository eventRepository;
    private final PurchasedTicketRepository purchasedTicketRepository;

    public List<Event> getEventsForReport() {
        return eventRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    public List<TicketTypeSalesReportDto> getTicketSalesByEvent(Integer eventId) {
        if (eventId == null) {
            return List.of();
        }
        return purchasedTicketRepository.findTicketSalesReportByEventId(eventId);
    }

    public long getTotalTicketsSold(List<TicketTypeSalesReportDto> ticketSales) {
        return ticketSales.stream()
                .map(TicketTypeSalesReportDto::getSoldTickets)
                .filter(value -> value != null)
                .mapToLong(Long::longValue)
                .sum();
    }

    public double getTotalRevenue(List<TicketTypeSalesReportDto> ticketSales) {
        return ticketSales.stream()
                .map(TicketTypeSalesReportDto::getTotalRevenue)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }
}