package co.edu.uptc.Ticketeo.reports.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketSalesByTypeRow {

    private final String ticketTypeName;
    private final Long soldTickets;
    private final Double totalRevenue;
}
