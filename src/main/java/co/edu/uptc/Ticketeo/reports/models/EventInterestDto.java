package co.edu.uptc.Ticketeo.reports.models;

import co.edu.uptc.Ticketeo.events.models.Event;
public interface EventInterestDto {
    Event getEvent();
    Long getTotalInterests();
}