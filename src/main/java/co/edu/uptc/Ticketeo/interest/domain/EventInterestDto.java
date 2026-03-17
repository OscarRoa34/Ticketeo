package co.edu.uptc.Ticketeo.interest.domain;

import co.edu.uptc.Ticketeo.catalog.domain.Event;
public interface EventInterestDto {
    Event getEvent();
    Long getTotalInterests();
}