package co.edu.uptc.Ticketeo.catalog.application;

import co.edu.uptc.Ticketeo.catalog.domain.Event;
import co.edu.uptc.Ticketeo.catalog.infrastructure.repository.EventRepository;
import co.edu.uptc.Ticketeo.interest.infrastructure.repository.InterestReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;

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

    public Page<Event> getActiveEventsFiltered(String search, Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCat = categoryId != null;
        if (hasSearch && hasCat) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(search, categoryId, pageable);
        } else if (hasCat) {
            return eventRepository.findByCategory_IdAndIsActiveTrue(categoryId, pageable);
        } else if (hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(search, pageable);
        }
        return eventRepository.findByIsActiveTrue(pageable);
    }

    public Page<Event> getInactiveEventsFiltered(String search, Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCat = categoryId != null;
        if (hasSearch && hasCat) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveFalse(search, categoryId, pageable);
        } else if (hasCat) {
            return eventRepository.findByCategory_IdAndIsActiveFalse(categoryId, pageable);
        } else if (hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveFalse(search, pageable);
        }
        return eventRepository.findByIsActiveFalse(pageable);
    }

    public Page<Event> getEventsFiltered(String searchQuery, Integer categoryId, int page, int size, String sortType) {
        Sort sort = resolveSort(sortType);
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
        eventRepository.findById(id).ifPresent(event -> {
            event.setIsActive(false);
            eventRepository.save(event);
        });
    }

    @Transactional
    public void reactivateEvent(Integer id) {
        eventRepository.findById(id).ifPresent(event -> {
            event.setIsActive(true);
            eventRepository.save(event);
        });
    }

    @Transactional
    public void deleteEvent(Integer id) {
        interestReportRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }

    private Sort resolveSort(String sortType) {
        if (sortType == null) return Sort.by("id").descending();
        return switch (sortType) {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "date_asc"   -> Sort.by("date").ascending();
            case "date_desc"  -> Sort.by("date").descending();
            default           -> Sort.by("id").descending();
        };
    }
}
