package co.edu.uptc.Ticketeo.reports.models;

public interface TicketTypeSalesReportDto {

    String getTicketTypeName();

    Long getSoldTickets();

    Double getTotalRevenue();
}