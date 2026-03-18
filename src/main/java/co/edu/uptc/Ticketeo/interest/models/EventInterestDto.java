package co.edu.uptc.Ticketeo.interest.models;

import co.edu.uptc.Ticketeo.events.models.Event;
public interface EventInterestDto {
    Event getEvent();
    Long getTotalInterests();
}