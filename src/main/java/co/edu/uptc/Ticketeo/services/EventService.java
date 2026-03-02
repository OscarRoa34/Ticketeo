package co.edu.uptc.Ticketeo.services;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.repository.EventRepository;
import co.edu.uptc.Ticketeo.repository.InterestReportRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;

    public EventService(EventRepository eventRepository, InterestReportRepository interestReportRepository) {
        this.eventRepository = eventRepository;
        this.interestReportRepository = interestReportRepository;
    }

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findByIsActiveTrue();
    }

    public Page<Event> getEventsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return eventRepository.findByIsActiveTrue(pageable);
    }

    public Page<Event> getInactiveEventsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return eventRepository.findByIsActiveFalse(pageable);
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
                case "date_asc":
                    sort = Sort.by("date").ascending();
                    break;
                case "date_desc":
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
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(searchQuery, categoryId, pageable);
        } else if (categoryId != null) {
            return eventRepository.findByCategory_IdAndIsActiveTrue(categoryId, pageable);
        } else if (searchQuery != null && !searchQuery.isEmpty()) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(searchQuery, pageable);
        } else {
            return eventRepository.findByIsActiveTrue(pageable);
        }
    }

    public List<Event> getRandomEvents(int count) {
        List<Event> activeEvents = eventRepository.findByIsActiveTrue();
        Collections.shuffle(activeEvents);
        return activeEvents.stream().limit(count).toList();
    }

    public Event getEventById(Integer id) {
        return eventRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deactivateEvent(Integer id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            event.setIsActive(false);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void reactivateEvent(Integer id) {
        Event event = eventRepository.findById(id).orElse(null);
        if (event != null) {
            event.setIsActive(true);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void deleteEvent(Integer id) {
        interestReportRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }
}
