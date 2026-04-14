package co.edu.uptc.Ticketeo.purchase.services;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchaseRepository;
import co.edu.uptc.Ticketeo.purchase.repositories.PurchasedTicketRepository;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchasedTicketRepository purchasedTicketRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    @Transactional
    public PurchaseCheckoutResult processPurchase(Integer eventId, String username, Map<Integer, Integer> requestedQuantities, String paymentMethodValue) {
        Event event = validateAndGetEvent(eventId);
        User user = validateAndGetUser(username);
        List<EventTicketType> assignments = findAssignments(eventId);
        Map<Integer, EventTicketType> assignmentByType = mapAssignmentsByTicketType(assignments);

        PurchaseCalculation calculation = calculateAndReserveTickets(requestedQuantities, assignmentByType, event);
        validateAtLeastOneTicket(calculation.totalTickets());

        saveAssignments(assignments);

        PaymentMethod paymentMethod = PaymentMethod.fromValue(paymentMethodValue);
        Purchase savedPurchase = savePurchase(user, event, paymentMethod, calculation.totalToPay(), calculation.totalTickets());

        savePurchasedTickets(savedPurchase, event, requestedQuantities, assignmentByType);
        eventService.recalculateMinimumAvailablePrice(eventId);

        return new PurchaseCheckoutResult(
                savedPurchase.getId(),
                calculation.totalTickets(),
                Math.round(calculation.totalToPay()),
                paymentMethod.name(),
                calculation.ticketTypeBreakdown()
        );
    }

    public List<Purchase> getUserPurchases(String username) {
        return purchaseRepository.findAllByUsernameWithTickets(username);
    }

    public Purchase getUserPurchase(Long purchaseId, String username) {
        return purchaseRepository.findByIdAndUsernameWithTickets(purchaseId, username).orElse(null);
    }

    public PurchasedTicket getUserTicket(Long ticketId, String username) {
        return purchasedTicketRepository.findByIdAndUsername(ticketId, username).orElse(null);
    }

    private Event validateAndGetEvent(Integer eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("Evento invalido.");
        }

        Event event = eventService.getEventById(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No se encontro el evento.");
        }
        if (eventService.isCompletedEvent(event)) {
            throw new IllegalArgumentException("No se pueden comprar boletas de eventos completados.");
        }
        return event;
    }

    private User validateAndGetUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el usuario."));
    }

    private List<EventTicketType> findAssignments(Integer eventId) {
        return eventTicketTypeRepository.findByEvent_Id(eventId);
    }

    private Map<Integer, EventTicketType> mapAssignmentsByTicketType(List<EventTicketType> assignments) {
        Map<Integer, EventTicketType> assignmentByType = new LinkedHashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment.getTicketType() != null && assignment.getTicketType().getId() != null) {
                assignmentByType.put(assignment.getTicketType().getId(), assignment);
            }
        }
        return assignmentByType;
    }

    private PurchaseCalculation calculateAndReserveTickets(
            Map<Integer, Integer> requestedQuantities,
            Map<Integer, EventTicketType> assignmentByType,
            Event event
    ) {
        int totalTickets = 0;
        double totalToPay = 0;
        Map<String, Integer> ticketTypeBreakdown = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> request : requestedQuantities.entrySet()) {
            Integer ticketTypeId = request.getKey();
            Integer quantity = request.getValue();
            if (quantity == null || quantity <= 0) {
                continue;
            }

            EventTicketType assignment = assignmentByType.get(ticketTypeId);
            validateAvailability(assignment, quantity);

            double ticketPrice = resolveTicketPrice(assignment, event);
            totalTickets += quantity;
            totalToPay += quantity * ticketPrice;
            assignment.setAvailableQuantity(assignment.getAvailableQuantity() - quantity);
            ticketTypeBreakdown.merge(resolveTicketTypeName(assignment), quantity, Integer::sum);
        }

        return new PurchaseCalculation(totalTickets, totalToPay, ticketTypeBreakdown);
    }

    private void validateAvailability(EventTicketType assignment, Integer quantity) {
        if (assignment == null || assignment.getAvailableQuantity() == null || quantity > assignment.getAvailableQuantity()) {
            throw new IllegalArgumentException("Una de las cantidades seleccionadas supera los boletos disponibles.");
        }
    }

    private void validateAtLeastOneTicket(int totalTickets) {
        if (totalTickets <= 0) {
            throw new IllegalArgumentException("Selecciona al menos un boleto para continuar.");
        }
    }

    private void saveAssignments(List<EventTicketType> assignments) {
        eventTicketTypeRepository.saveAll(assignments);
    }

    private Purchase savePurchase(User user, Event event, PaymentMethod paymentMethod, double totalToPay, int totalTickets) {
        Purchase purchase = Purchase.builder()
                .user(user)
                .eventId(event.getId())
                .eventName(event.getName())
                .eventDate(event.getDate())
                .paymentMethod(paymentMethod)
                .purchaseDate(LocalDateTime.now())
                .totalPaid(totalToPay)
                .totalTickets(totalTickets)
                .build();
        return purchaseRepository.save(purchase);
    }

    private void savePurchasedTickets(
            Purchase purchase,
            Event event,
            Map<Integer, Integer> requestedQuantities,
            Map<Integer, EventTicketType> assignmentByType
    ) {
        for (Map.Entry<Integer, Integer> request : requestedQuantities.entrySet()) {
            EventTicketType assignment = assignmentByType.get(request.getKey());
            Integer quantity = request.getValue();
            if (assignment == null || quantity == null || quantity <= 0) {
                continue;
            }

            double ticketPrice = resolveTicketPrice(assignment, event);
            String ticketTypeName = resolveTicketTypeName(assignment);
            for (int i = 0; i < quantity; i++) {
                PurchasedTicket ticket = PurchasedTicket.builder()
                        .purchase(purchase)
                        .ticketTypeName(ticketTypeName)
                        .unitPrice(ticketPrice)
                        .qrCode(generateTicketCode(purchase.getId(), event.getId()))
                        .build();
                purchasedTicketRepository.save(ticket);
            }
        }
    }

    private String resolveTicketTypeName(EventTicketType assignment) {
        return assignment.getTicketType() != null ? assignment.getTicketType().getName() : "Boleto";
    }

    private double resolveUnitPrice(Event event) {
        return event.getPrice() == null ? 0.0 : event.getPrice();
    }

    private double resolveTicketPrice(EventTicketType eventTicketType, Event event) {
        if (eventTicketType.getTicketPrice() != null && eventTicketType.getTicketPrice() > 0) {
            return eventTicketType.getTicketPrice();
        }
        return resolveUnitPrice(event);
    }

    private String generateTicketCode(Long purchaseId, Integer eventId) {
        return "TK-" + purchaseId + "-" + eventId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record PurchaseCalculation(int totalTickets, double totalToPay, Map<String, Integer> ticketTypeBreakdown) {
    }

    public record PurchaseCheckoutResult(Long purchaseId, Integer tickets, Long total, String paymentMethod,
                                         Map<String, Integer> ticketTypeBreakdown) {
    }
}

