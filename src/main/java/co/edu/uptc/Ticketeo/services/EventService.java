package co.edu.uptc.Ticketeo.services;


import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventById(Integer id) {
        return eventRepository.findById(id).orElse(null);
    }

    public void deleteEvent(Integer id) {
        eventRepository.deleteById(id);
    }
}


