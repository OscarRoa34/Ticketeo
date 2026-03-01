package co.edu.uptc.Ticketeo.services;


import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.repository.EventRepository;

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

    public Page<Event> getEventsFiltered(String searchQuery, Integer categoryId, int page, int size, String sortType) {
        Sort sort = Sort.unsorted();

        if (sortType != null) {
            switch (sortType) {
                case "price_asc":
                    sort = Sort.by("price").ascending();
                    break;
                case "price_desc":
                    sort = Sort.by("price").descending();
                    break;
                case "date_asc": // Próximos
                    sort = Sort.by("date").ascending();
                    break;
                case "date_desc": // Recientes
                    sort = Sort.by("date").descending();
                    break;
                default:
                    sort = Sort.by("id").descending();
            }
        } else {
             sort = Sort.by("id").descending();
        }

        Pageable pageable = PageRequest.of(page, size, sort);

        if (categoryId != null && searchQuery != null && !searchQuery.isEmpty()) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_Id(searchQuery, categoryId, pageable);
        } else if (categoryId != null) {
            return eventRepository.findByCategory_Id(categoryId, pageable);
        } else if (searchQuery != null && !searchQuery.isEmpty()) {
            return eventRepository.findByNameContainingIgnoreCase(searchQuery, pageable);
        } else {
            return eventRepository.findAll(pageable);
        }
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


