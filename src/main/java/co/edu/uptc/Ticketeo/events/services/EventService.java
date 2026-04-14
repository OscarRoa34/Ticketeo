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
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.reports.repositories.InterestReportRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final PurchasedTicketRepository purchasedTicketRepository;

    @Transactional
    public Event saveEventWithTicketTypes(Event event, Map<Integer, Integer> ticketTypeQuantities, Map<Integer, Double> ticketTypePrices) {
        validateEventDateNotPast(event);

        Event savedEvent = eventRepository.save(event);
        TicketTypeRequest ticketTypeRequest = normalizeTicketTypeRequest(ticketTypeQuantities, ticketTypePrices);
        List<EventTicketType> currentAssignments = loadCurrentAssignments(savedEvent.getId());
        Set<Integer> lockedTypeIds = validateLockedTicketTypes(currentAssignments, savedEvent.getId(), ticketTypeRequest);

        persistEventTicketTypes(savedEvent, ticketTypeRequest, lockedTypeIds);
        recalculateMinimumAvailablePrice(savedEvent.getId());
        return reloadEvent(savedEvent);
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

    public Page<Event> getActiveEventsFiltered(String search, Integer categoryId, int page, int size) {
        return findActiveEvents(search, categoryId, LocalDate.now(), idDescPageable(page, size));
    }

    public Page<Event> getCompletedEventsFiltered(String search, Integer categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending().and(Sort.by("id").descending()));
        return findCompletedEvents(search, categoryId, LocalDate.now(), pageable);
    }

    public Page<Event> getInactiveEventsFiltered(String search, Integer categoryId, int page, int size) {
        return findInactiveEvents(search, categoryId, idDescPageable(page, size));
    }

    public Page<Event> getEventsFiltered(String searchQuery, Integer categoryId, int page, int size, String sortType) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortType));
        if ("date_asc".equals(sortType)) {
            return findPublicEventsFromToday(searchQuery, categoryId, LocalDate.now(), pageable);
        }
        return findPublicEvents(searchQuery, categoryId, pageable);
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
            case "date_asc" -> Sort.by(Sort.Order.asc("date"), Sort.Order.asc("id"));
            case "date_desc" -> Sort.by(Sort.Order.desc("createdAt").nullsLast(), Sort.Order.desc("id"));
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

        Map<String, Long> soldCountByTypeName = loadSoldCountByTicketTypeName(eventId);

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

    private void validateEventDateNotPast(Event event) {
        if (event != null && event.getDate() != null && event.getDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del evento no puede ser anterior a hoy.");
        }
    }

    private TicketTypeRequest normalizeTicketTypeRequest(Map<Integer, Integer> quantities, Map<Integer, Double> prices) {
        Map<Integer, Integer> safeQuantities = quantities != null ? new HashMap<>(quantities) : new HashMap<>();
        Map<Integer, Double> safePrices = prices != null ? new HashMap<>(prices) : new HashMap<>();
        return new TicketTypeRequest(safeQuantities, safePrices);
    }

    private List<EventTicketType> loadCurrentAssignments(Integer eventId) {
        if (eventId == null) {
            return List.of();
        }
        return eventTicketTypeRepository.findByEvent_Id(eventId);
    }

    private Set<Integer> validateLockedTicketTypes(List<EventTicketType> currentAssignments,
                                                    Integer eventId,
                                                    TicketTypeRequest ticketTypeRequest) {
        if (eventId == null || currentAssignments.isEmpty()) {
            return Set.of();
        }

        Map<Integer, Boolean> soldTicketTypes = getSoldTicketTypesForEvent(eventId);
        Set<Integer> lockedTypeIds = new HashSet<>();

        for (EventTicketType current : currentAssignments) {
            Integer typeId = current.getTicketType() != null ? current.getTicketType().getId() : null;
            if (typeId == null || !Boolean.TRUE.equals(soldTicketTypes.get(typeId))) {
                continue;
            }

            validateLockedQuantity(typeId, current, ticketTypeRequest.quantities());
            validateLockedPrice(typeId, current, ticketTypeRequest.prices());
            lockedTypeIds.add(typeId);
        }
        return lockedTypeIds;
    }

    private void validateLockedQuantity(Integer typeId, EventTicketType current, Map<Integer, Integer> requestedQuantities) {
        Integer requestedQty = requestedQuantities.get(typeId);
        if (requestedQty == null || !requestedQty.equals(current.getAvailableQuantity())) {
            throw new IllegalArgumentException("No se puede modificar la cantidad de un tipo de boleta que ya tiene ventas.");
        }
    }

    private void validateLockedPrice(Integer typeId, EventTicketType current, Map<Integer, Double> requestedPrices) {
        Double requestedPrice = requestedPrices.get(typeId);
        if (requestedPrice == null || Double.compare(requestedPrice, current.getTicketPrice()) != 0) {
            throw new IllegalArgumentException("No se puede modificar el precio de un tipo de boleta que ya tiene ventas.");
        }
    }

    private void persistEventTicketTypes(Event savedEvent, TicketTypeRequest ticketTypeRequest, Set<Integer> lockedTypeIds) {
        Integer eventId = savedEvent.getId();
        eventTicketTypeRepository.deleteByEvent_Id(eventId);

        if (ticketTypeRequest.quantities().isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> quantityEntry : ticketTypeRequest.quantities().entrySet()) {
            Integer ticketTypeId = quantityEntry.getKey();
            Integer quantity = quantityEntry.getValue();
            Double ticketPrice = ticketTypeRequest.prices().get(ticketTypeId);
            boolean isLockedType = lockedTypeIds.contains(ticketTypeId);

            if (!isValidTicketTypeInput(ticketTypeId, quantity, ticketPrice, isLockedType)) {
                continue;
            }

            TicketType ticketType = ticketTypeRepository.findById(ticketTypeId).orElse(null);
            if (ticketType == null) {
                continue;
            }

            eventTicketTypeRepository.save(EventTicketType.builder()
                    .event(savedEvent)
                    .ticketType(ticketType)
                    .availableQuantity(quantity)
                    .ticketPrice(ticketPrice)
                    .build());
        }
    }

    private boolean isValidTicketTypeInput(Integer ticketTypeId, Integer quantity, Double ticketPrice, boolean isLockedType) {
        if (ticketTypeId == null || quantity == null || ticketPrice == null) {
            return false;
        }
        if (!isLockedType) {
            return quantity > 0 && ticketPrice > 0;
        }
        return quantity >= 0 && ticketPrice >= 0;
    }

    private Event reloadEvent(Event savedEvent) {
        Integer eventId = savedEvent.getId();
        if (eventId == null) {
            return savedEvent;
        }
        return eventRepository.findById(eventId).orElse(savedEvent);
    }

    private Pageable idDescPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("id").descending());
    }

    private Page<Event> findActiveEvents(String search, Integer categoryId, LocalDate today, Pageable pageable) {
        boolean hasSearch = hasText(search);
        boolean hasCategory = categoryId != null;

        if (hasSearch && hasCategory) {
            return eventRepository.findManageableActiveEventsByNameAndCategory(search, categoryId, today, pageable);
        }
        if (hasCategory) {
            return eventRepository.findManageableActiveEventsByCategory(categoryId, today, pageable);
        }
        if (hasSearch) {
            return eventRepository.findManageableActiveEventsByName(search, today, pageable);
        }
        return eventRepository.findManageableActiveEvents(today, pageable);
    }

    private Page<Event> findCompletedEvents(String search, Integer categoryId, LocalDate today, Pageable pageable) {
        boolean hasSearch = hasText(search);
        boolean hasCategory = categoryId != null;

        if (hasSearch && hasCategory) {
            return eventRepository.findCompletedEventsByNameAndCategory(search, categoryId, today, pageable);
        }
        if (hasCategory) {
            return eventRepository.findCompletedEventsByCategory(categoryId, today, pageable);
        }
        if (hasSearch) {
            return eventRepository.findCompletedEventsByName(search, today, pageable);
        }
        return eventRepository.findCompletedEvents(today, pageable);
    }

    private Page<Event> findInactiveEvents(String search, Integer categoryId, Pageable pageable) {
        boolean hasSearch = hasText(search);
        boolean hasCategory = categoryId != null;

        if (hasSearch && hasCategory) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveFalse(search, categoryId, pageable);
        }
        if (hasCategory) {
            return eventRepository.findByCategory_IdAndIsActiveFalse(categoryId, pageable);
        }
        if (hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveFalse(search, pageable);
        }
        return eventRepository.findByIsActiveFalse(pageable);
    }

    private Page<Event> findPublicEventsFromToday(String searchQuery, Integer categoryId, LocalDate today, Pageable pageable) {
        boolean hasSearch = hasText(searchQuery);
        boolean hasCategory = categoryId != null;

        if (hasCategory && hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(searchQuery, categoryId, today, pageable);
        }
        if (hasCategory) {
            return eventRepository.findByCategory_IdAndIsActiveTrueAndDateGreaterThanEqual(categoryId, today, pageable);
        }
        if (hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrueAndDateGreaterThanEqual(searchQuery, today, pageable);
        }
        return eventRepository.findByIsActiveTrueAndDateGreaterThanEqual(today, pageable);
    }

    private Page<Event> findPublicEvents(String searchQuery, Integer categoryId, Pageable pageable) {
        boolean hasSearch = hasText(searchQuery);
        boolean hasCategory = categoryId != null;

        if (hasCategory && hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndCategory_IdAndIsActiveTrue(searchQuery, categoryId, pageable);
        }
        if (hasCategory) {
            return eventRepository.findByCategory_IdAndIsActiveTrue(categoryId, pageable);
        }
        if (hasSearch) {
            return eventRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(searchQuery, pageable);
        }
        return eventRepository.findByIsActiveTrue(pageable);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Long> loadSoldCountByTicketTypeName(Integer eventId) {
        Map<String, Long> soldCountByTypeName = new HashMap<>();
        for (Object[] row : purchasedTicketRepository.countSoldTicketsByTypeNameForEvent(eventId)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            soldCountByTypeName.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return soldCountByTypeName;
    }

    private record TicketTypeRequest(Map<Integer, Integer> quantities, Map<Integer, Double> prices) {
    }
}
