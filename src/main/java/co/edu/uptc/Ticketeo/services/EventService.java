package co.edu.uptc.Ticketeo.services;


import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    public Page<Event> getEventsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(pageable);
    }

    public List<Event> getRandomEvents(int count) {
        List<Event> allEvents = eventRepository.findAll();
        Collections.shuffle(allEvents);
        return allEvents.stream().limit(count).toList();
    }

    public Event getEventById(Integer id) {
        return eventRepository.findById(id).orElse(null);
    }

    public void deleteEvent(Integer id) {
        eventRepository.deleteById(id);
    }
}


