package co.edu.uptc.Ticketeo.services;


import co.edu.uptc.Ticketeo.models.EventModel;
import co.edu.uptc.Ticketeo.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventModel saveEvent(EventModel event) {
        return eventRepository.save(event);
    }

    public List<EventModel> getAllEvents() {
        return eventRepository.findAll();
    }

    public void deleteEvent(Long id) {
        eventRepository.deleteById(id);
    }
}


