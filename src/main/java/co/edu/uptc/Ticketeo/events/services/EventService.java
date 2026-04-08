package co.edu.uptc.Ticketeo.events.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import co.edu.uptc.Ticketeo.reports.repositories.InterestReportRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PurchasedTicketRepository purchasedTicketRepository;

    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional
    public Event saveEventWithTicketTypes(Event event, Map<Integer, Integer> ticketTypeQuantities, Map<Integer, Double> ticketTypePrices) {
        if (event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del evento no puede ser anterior a hoy.");
        }

        Event savedEvent = eventRepository.save(event);

        Map<Integer, Integer> safeQuantities = ticketTypeQuantities != null ? new HashMap<>(ticketTypeQuantities) : new HashMap<>();
        Map<Integer, Double> safePrices = ticketTypePrices != null ? new HashMap<>(ticketTypePrices) : new HashMap<>();

        List<EventTicketType> currentAssignments = savedEvent.getId() != null
                ? eventTicketTypeRepository.findByEvent_Id(savedEvent.getId())
                : List.of();

        Map<Integer, Boolean> soldTicketTypes = getSoldTicketTypesForEvent(savedEvent.getId());
        Set<Integer> lockedTypeIds = new HashSet<>();
        for (EventTicketType current : currentAssignments) {
            Integer typeId = current.getTicketType() != null ? current.getTicketType().getId() : null;
            if (typeId == null || !Boolean.TRUE.equals(soldTicketTypes.get(typeId))) {
                continue;
            }

            Integer requestedQty = safeQuantities.get(typeId);
            Double requestedPrice = safePrices.get(typeId);
            if (requestedQty == null || !requestedQty.equals(current.getAvailableQuantity())) {
                throw new IllegalArgumentException("No se puede modificar la cantidad de un tipo de boleta que ya tiene ventas.");
            }
            if (requestedPrice == null || Double.compare(requestedPrice, current.getTicketPrice()) != 0) {
                throw new IllegalArgumentException("No se puede modificar el precio de un tipo de boleta que ya tiene ventas.");
            }
            lockedTypeIds.add(typeId);
        }

        eventTicketTypeRepository.deleteByEvent_Id(savedEvent.getId());

        if (safeQuantities.isEmpty()) {
            recalculateMinimumAvailablePrice(savedEvent.getId());
            return eventRepository.findById(savedEvent.getId()).orElse(savedEvent);
        }

        for (Map.Entry<Integer, Integer> entry : safeQuantities.entrySet()) {
            Integer ticketTypeId = entry.getKey();
            Integer quantity = entry.getValue();
            Double ticketPrice = safePrices.get(ticketTypeId);
            boolean isLockedType = lockedTypeIds.contains(ticketTypeId);
            if (ticketTypeId == null || quantity == null || ticketPrice == null) {
                continue;
            }
            if (!isLockedType && (quantity <= 0 || ticketPrice <= 0)) {
                continue;
            }
            if (isLockedType && (quantity < 0 || ticketPrice < 0)) {
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
                    .ticketPrice(ticketPrice)
                    .build();
            eventTicketTypeRepository.save(eventTicketType);
        }

        recalculateMinimumAvailablePrice(savedEvent.getId());
        return eventRepository.findById(savedEvent.getId()).orElse(savedEvent);
    }

    @Transactional
    public void recalculateMinimumAvailablePrice(Integer eventId) {
        if (eventId == null) {
            return;
        }

        eventRepository.findById(eventId).ifPresent(event -> {
            Double minimumAvailablePrice = eventTicketTypeRepository.findMinimumAvailableTicketPriceByEventId(eventId);
            event.setPrice(minimumAvailablePrice);
            eventRepository.save(event);
        });
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

    public Map<Integer, Double> getTicketTypePricesForEvent(Integer eventId) {
        if (eventId == null) {
            return Map.of();
        }

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(eventId);
        Map<Integer, Double> prices = new HashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment.getTicketType() != null && assignment.getTicketType().getId() != null) {
                prices.put(assignment.getTicketType().getId(), assignment.getTicketPrice());
            }
        }
        return prices;
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
        activeEvents = new java.util.ArrayList<>(activeEvents.stream()
                .filter(event -> !isCompletedEvent(event))
                .toList());
        Collections.shuffle(activeEvents);
        return activeEvents.stream().limit(count).toList();
    }

    public Event getEventById(Integer id) {
        return eventRepository.findById(id).orElse(null);
    }

    public boolean isCompletedEvent(Event event) {
        return event != null
                && event.getDate() != null
                && event.getDate().isBefore(LocalDate.now());
    }

    public boolean hasAvailableTicketsForEvent(Integer eventId) {
        if (eventId == null) {
            return false;
        }
        return eventTicketTypeRepository.existsByEvent_IdAndAvailableQuantityGreaterThan(eventId, 0);
    }

    @Transactional
    public void deactivateEvent(Integer id) {
        if (purchasedTicketRepository.existsByPurchase_EventId(id)) {
            throw new IllegalArgumentException("No se puede desactivar un evento que ya tiene boletas vendidas.");
        }
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
        if (sortType == null) {
            return Sort.by(Sort.Order.desc("id"));
        }
        return switch (sortType) {
            case "price_asc" -> Sort.by(Sort.Order.asc("price").nullsLast(), Sort.Order.desc("date"), Sort.Order.desc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("price").nullsLast(), Sort.Order.desc("date"), Sort.Order.desc("id"));
            case "date_asc" -> Sort.by(Sort.Order.asc("date"), Sort.Order.desc("id"));
            case "date_desc" -> Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));
            default -> Sort.by(Sort.Order.desc("id"));
        };
    }


    public Map<Integer, Boolean> getSoldTicketTypesForEvent(Integer eventId) {
        if (eventId == null) {
            return Map.of();
        }

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(eventId);
        if (assignments.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> soldCountByTypeName = new HashMap<>();
        for (Object[] row : purchasedTicketRepository.countSoldTicketsByTypeNameForEvent(eventId)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            soldCountByTypeName.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<Integer, Boolean> soldByTypeId = new HashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment.getTicketType() == null || assignment.getTicketType().getId() == null) {
                continue;
            }
            String typeName = assignment.getTicketType().getName();
            boolean hasSales = typeName != null && soldCountByTypeName.getOrDefault(typeName, 0L) > 0;
            soldByTypeId.put(assignment.getTicketType().getId(), hasSales);
        }
        return soldByTypeId;
    }
}
