package co.edu.uptc.Ticketeo.reports.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.reports.models.EventInterestDto;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesReportDto;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
import co.edu.uptc.Ticketeo.reports.services.TicketSalesReportService;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    @Mock
    private InterestReportService interestReportService;

    @Mock
    private TicketSalesReportService ticketSalesReportService;

    @InjectMocks
    private AdminReportController adminReportController;

    @Test
    void showReportsMenu_returnsMenuView() {
        assertEquals("reports/adminReportsMenu", adminReportController.showReportsMenu());
    }

    @Test
    void showInterestReport_loadsRankingInModel() {
        Event event = new Event();
        event.setId(3);
        EventInterestDto ranking = new EventInterestDto() {
            @Override
            public Event getEvent() {
                return event;
            }

            @Override
            public Long getTotalInterests() {
                return 10L;
            }
        };

        when(interestReportService.getEventInterestRanking()).thenReturn(List.of(ranking));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminReportController.showInterestReport(model);

        assertEquals("reports/adminInterestReport", view);
        assertEquals(List.of(ranking), model.get("interestRanking"));
    }

    @Test
    void showTicketSalesReportByEvent_withoutEventId_returnsDefaults() {
        Event event = new Event();
        event.setId(9);
        when(ticketSalesReportService.getEventsForReport()).thenReturn(List.of(event));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminReportController.showTicketSalesReportByEvent(null, model);

        assertEquals("reports/adminTicketSalesReportByEvent", view);
        assertEquals(List.of(event), model.get("events"));
        assertEquals(List.of(), model.get("ticketSales"));
        assertNull(model.get("selectedEvent"));
        assertEquals(0L, model.get("totalTicketsSold"));
        assertEquals(0.0, model.get("totalRevenue"));
    }

    @Test
    void showTicketSalesReportByEvent_withEventId_loadsSelectionAndTotals() {
        Event event = new Event();
        event.setId(12);
        TicketTypeSalesReportDto dto = new TicketTypeSalesReportDto() {
            @Override
            public String getTicketTypeName() {
                return "Platea";
            }

            @Override
            public Long getSoldTickets() {
                return 5L;
            }

            @Override
            public Double getTotalRevenue() {
                return 250000.0;
            }
        };

        when(ticketSalesReportService.getEventsForReport()).thenReturn(List.of(event));
        when(ticketSalesReportService.getTicketSalesByEvent(12)).thenReturn(List.of(dto));
        when(ticketSalesReportService.getTotalTicketsSold(List.of(dto))).thenReturn(5L);
        when(ticketSalesReportService.getTotalRevenue(List.of(dto))).thenReturn(250000.0);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminReportController.showTicketSalesReportByEvent(12, model);

        assertEquals("reports/adminTicketSalesReportByEvent", view);
        assertEquals(event, model.get("selectedEvent"));
        assertEquals(12, model.get("selectedEventId"));
        assertEquals(List.of(dto), model.get("ticketSales"));
        assertEquals(5L, model.get("totalTicketsSold"));
        assertEquals(250000.0, model.get("totalRevenue"));
    }
}
