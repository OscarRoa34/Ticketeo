package co.edu.uptc.Ticketeo.reports.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesReportDto;

@ExtendWith(MockitoExtension.class)
class TicketSalesReportServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PurchasedTicketRepository purchasedTicketRepository;

    @InjectMocks
    private TicketSalesReportService ticketSalesReportService;

    @Test
    void getEventsForReport_returnsSortedEvents() {
        Event event = new Event();
        event.setId(1);
        when(eventRepository.findAll(Sort.by(Sort.Order.asc("name")))).thenReturn(List.of(event));

        List<Event> events = ticketSalesReportService.getEventsForReport();

        assertEquals(1, events.size());
        assertEquals(1, events.get(0).getId());
    }

    @Test
    void getTicketSalesByEvent_whenEventIdNull_returnsEmptyList() {
        assertEquals(0, ticketSalesReportService.getTicketSalesByEvent(null).size());
    }

    @Test
    void getTicketSalesByEvent_whenEventIdPresent_queriesRepository() {
        TicketTypeSalesReportDto dto = ticketDto("General", 3L, 120000.0);
        when(purchasedTicketRepository.findTicketSalesReportByEventId(5)).thenReturn(List.of(dto));

        List<TicketTypeSalesReportDto> report = ticketSalesReportService.getTicketSalesByEvent(5);

        assertEquals(1, report.size());
        assertEquals("General", report.get(0).getTicketTypeName());
    }

    @Test
    void getTotalTicketsSold_ignoresNullValues() {
        List<TicketTypeSalesReportDto> rows = List.of(
                ticketDto("General", 3L, 1000.0),
                ticketDto("VIP", null, 2000.0),
                ticketDto("Platea", 2L, null));

        long total = ticketSalesReportService.getTotalTicketsSold(rows);

        assertEquals(5L, total);
    }

    @Test
    void getTotalRevenue_ignoresNullValues() {
        List<TicketTypeSalesReportDto> rows = List.of(
                ticketDto("General", 3L, 1000.0),
                ticketDto("VIP", null, 2000.5),
                ticketDto("Platea", 2L, null));

        double total = ticketSalesReportService.getTotalRevenue(rows);

        assertEquals(3000.5, total);
    }

    private TicketTypeSalesReportDto ticketDto(String typeName, Long sold, Double revenue) {
        return new TicketTypeSalesReportDto() {
            @Override
            public String getTicketTypeName() {
                return typeName;
            }

            @Override
            public Long getSoldTickets() {
                return sold;
            }

            @Override
            public Double getTotalRevenue() {
                return revenue;
            }
        };
    }
}

