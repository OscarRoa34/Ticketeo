package co.edu.uptc.Ticketeo.reports.services;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.reports.models.TicketSalesByTypeRow;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesAggregationDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketSalesReportService {

    private final EventRepository eventRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PurchasedTicketRepository purchasedTicketRepository;

    public List<Event> getEventsForReportSelection() {
        return eventRepository.findAll(Sort.by(Sort.Order.asc("name")));
    }

    public Event getEventById(Integer eventId) {
        if (eventId == null) {
            return null;
        }
        return eventRepository.findById(eventId).orElse(null);
    }

    public List<TicketSalesByTypeRow> getTicketSalesByTypeForEvent(Integer eventId) {
        if (eventId == null) {
            return List.of();
        }

        List<EventTicketType> ticketTypeAssignments = eventTicketTypeRepository.findByEvent_Id(eventId);
        if (ticketTypeAssignments.isEmpty()) {
            return List.of();
        }

        Map<String, TicketTypeSalesAggregationDto> summaryByTypeName = new LinkedHashMap<>();
        for (TicketTypeSalesAggregationDto summary : purchasedTicketRepository.summarizeTicketSalesByTypeForEvent(eventId)) {
            if (summary == null || summary.getTicketTypeName() == null) {
                continue;
            }
            summaryByTypeName.put(summary.getTicketTypeName(), summary);
        }

        return ticketTypeAssignments.stream()
                .sorted(Comparator.comparing(this::resolveTicketTypeName, String.CASE_INSENSITIVE_ORDER))
                .map(assignment -> {
                    String ticketTypeName = resolveTicketTypeName(assignment);
                    TicketTypeSalesAggregationDto summary = summaryByTypeName.get(ticketTypeName);
                    Long soldTickets = summary != null ? summary.getSoldTickets() : null;
                    Double totalRevenue = summary != null ? summary.getTotalRevenue() : null;
                    return new TicketSalesByTypeRow(
                            ticketTypeName,
                            defaultSoldTickets(soldTickets),
                            defaultTotalRevenue(totalRevenue)
                    );
                })
                .toList();
    }

    private Long defaultSoldTickets(Long soldTickets) {
        return soldTickets == null ? 0L : soldTickets;
    }

    private Double defaultTotalRevenue(Double totalRevenue) {
        return totalRevenue == null ? 0.0 : totalRevenue;
    }

    private String resolveTicketTypeName(EventTicketType assignment) {
        if (assignment == null || assignment.getTicketType() == null || assignment.getTicketType().getName() == null) {
            return "Boleto";
        }
        return assignment.getTicketType().getName();
    }
}
