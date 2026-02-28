package co.edu.uptc.Ticketeo.dtos;

import co.edu.uptc.Ticketeo.models.Event;
public interface EventInterestDto {
    Event getEvent();
    Long getTotalInterests();
}