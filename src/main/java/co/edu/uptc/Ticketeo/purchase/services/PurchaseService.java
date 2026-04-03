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

        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("No se encontro el usuario."));

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(eventId);
        Map<Integer, EventTicketType> assignmentByType = new LinkedHashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment.getTicketType() != null && assignment.getTicketType().getId() != null) {
                assignmentByType.put(assignment.getTicketType().getId(), assignment);
            }
        }

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
            if (assignment == null || assignment.getAvailableQuantity() == null || quantity > assignment.getAvailableQuantity()) {
                throw new IllegalArgumentException("Una de las cantidades seleccionadas supera los boletos disponibles.");
            }

            double ticketPrice = resolveTicketPrice(assignment, event);
            totalTickets += quantity;
            totalToPay += quantity * ticketPrice;
            assignment.setAvailableQuantity(assignment.getAvailableQuantity() - quantity);

            String ticketTypeName = assignment.getTicketType() != null ? assignment.getTicketType().getName() : "Boleto";
            ticketTypeBreakdown.merge(ticketTypeName, quantity, Integer::sum);
        }

        if (totalTickets <= 0) {
            throw new IllegalArgumentException("Selecciona al menos un boleto para continuar.");
        }

        eventTicketTypeRepository.saveAll(assignments);

        PaymentMethod paymentMethod = PaymentMethod.fromValue(paymentMethodValue);
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
        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (Map.Entry<Integer, Integer> request : requestedQuantities.entrySet()) {
            EventTicketType assignment = assignmentByType.get(request.getKey());
            Integer quantity = request.getValue();
            if (assignment == null || quantity == null || quantity <= 0) {
                continue;
            }

            double ticketPrice = resolveTicketPrice(assignment, event);
            String ticketTypeName = assignment.getTicketType() != null ? assignment.getTicketType().getName() : "Boleto";
            for (int i = 0; i < quantity; i++) {
                PurchasedTicket ticket = PurchasedTicket.builder()
                        .purchase(savedPurchase)
                        .ticketTypeName(ticketTypeName)
                        .unitPrice(ticketPrice)
                        .qrCode(generateTicketCode(savedPurchase.getId(), event.getId()))
                        .build();
                purchasedTicketRepository.save(ticket);
            }
        }

        eventService.recalculateMinimumAvailablePrice(eventId);

        return new PurchaseCheckoutResult(savedPurchase.getId(), totalTickets, Math.round(totalToPay), paymentMethod.name(), ticketTypeBreakdown);
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

    public record PurchaseCheckoutResult(Long purchaseId, Integer tickets, Long total, String paymentMethod,
                                         Map<String, Integer> ticketTypeBreakdown) {
    }
}

