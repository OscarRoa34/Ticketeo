package co.edu.uptc.Ticketeo.events.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventRepository;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.repositories.TicketTypeRepository;
import co.edu.uptc.Ticketeo.interest.repositories.InterestReportRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional
    public Event saveEventWithTicketTypes(Event event, Map<Integer, Integer> ticketTypeQuantities) {
        Event savedEvent = eventRepository.save(event);
        eventTicketTypeRepository.deleteByEvent_Id(savedEvent.getId());

        if (ticketTypeQuantities == null || ticketTypeQuantities.isEmpty()) {
            return savedEvent;
        }

        for (Map.Entry<Integer, Integer> entry : ticketTypeQuantities.entrySet()) {
            Integer ticketTypeId = entry.getKey();
            Integer quantity = entry.getValue();
            if (ticketTypeId == null || quantity == null || quantity <= 0) {
                continue;
            }

            TicketType ticketType = ticketTypeRepository.findById(ticketTypeId).orElse(null);
            if (ticketType == null) {
                continue;
            }

            EventTicketType eventTicketType = EventTicketType.builder()
                    .event(savedEvent)
                    .ticketType(ticketType)
                    .availableQuantity(quantity)
                    .build();
            eventTicketTypeRepository.save(eventTicketType);
        }
        return savedEvent;
    }

    public Map<Integer, Integer> getTicketTypeQuantitiesForEvent(Integer eventId) {
        if (eventId == null) {
            return Map.of();
        }

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(eventId);
        Map<Integer, Integer> quantities = new HashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment.getTicketType() != null && assignment.getTicketType().getId() != null) {
                quantities.put(assignment.getTicketType().getId(), assignment.getAvailableQuantity());
            }
        }
        return quantities;
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
        LocalDate today = LocalDate.now();
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCat = categoryId != null;

        if (hasSearch && hasCat) {
            return eventRepository.findManageableActiveEventsByNameAndCategory(search, categoryId, today, pageable);
        } else if (hasCat) {
            return eventRepository.findManageableActiveEventsByCategory(categoryId, today, pageable);
        } else if (hasSearch) {
            return eventRepository.findManageableActiveEventsByName(search, today, pageable);
        }
        return eventRepository.findManageableActiveEvents(today, pageable);
    }

    public Page<Event> getCompletedEventsFiltered(String search, Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending().and(Sort.by("id").descending()));
        LocalDate today = LocalDate.now();
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasCat = categoryId != null;

        if (hasSearch && hasCat) {
            return eventRepository.findCompletedEventsByNameAndCategory(search, categoryId, today, pageable);
        } else if (hasCat) {
            return eventRepository.findCompletedEventsByCategory(categoryId, today, pageable);
        } else if (hasSearch) {
            return eventRepository.findCompletedEventsByName(search, today, pageable);
        }
        return eventRepository.findCompletedEvents(today, pageable);
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
        eventTicketTypeRepository.deleteByEvent_Id(id);
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
