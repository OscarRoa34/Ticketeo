package co.edu.uptc.Ticketeo.reports.models;

public interface TicketTypeSalesAggregationDto {
    String getTicketTypeName();

    Long getSoldTickets();

    Double getTotalRevenue();
}
