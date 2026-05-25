package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.services.PaymentProcessingService;
import co.edu.uptc.Ticketeo.purchase.services.PaymentResult;
import co.edu.uptc.Ticketeo.purchase.services.PaymentTrackingService;
import co.edu.uptc.Ticketeo.purchase.services.PendingPaymentRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventPurchaseController {

    private static final String VIEW_EVENT_PURCHASE = "events/eventPurchase";
    private static final String VIEW_PAYMENT_SUCCESS = "events/paymentSuccess";
    private static final String VIEW_PAYMENT_PROCESSING = "events/paymentProcessing";
    private static final DateTimeFormatter PURCHASE_JSON_DATE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Duration CHECKEO_TIMEOUT = Duration.ofSeconds(30);
    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentTrackingService paymentTrackingService;

    @GetMapping("/{id}/purchase")
    public String showPurchaseForm(@PathVariable Integer id,
                                   @RequestParam(required = false) String trackingId,
                                   Authentication authentication,
                                   Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }

        String purchaseError = null;
        if (eventService.isCompletedEvent(event)) {
            purchaseError = "Este evento ya fue completado.";
        } else if (!eventService.hasAvailableTicketsForEvent(id)) {
            purchaseError = "No hay boletos disponibles para este evento.";
        }

        populatePurchaseModel(model, event, purchaseError);
        if (trackingId != null && !trackingId.isBlank()) {
            paymentTrackingService.getPending(trackingId).ifPresent(pending -> {
                if (pending.eventId() != null && pending.eventId().equals(id)) {
                    model.addAttribute("prefillQuantities", pending.quantities());
                    model.addAttribute("prefillCardBrand", pending.cardBrand());
                    model.addAttribute("prefillCardNumber", pending.cardNumber());
                    model.addAttribute("prefillCardCvv", pending.cardCvv());
                }
            });
        }
        return VIEW_EVENT_PURCHASE;
    }

    @PostMapping("/{id}/purchase/pay")
    public String processPurchase(@PathVariable Integer id,
                                  Authentication authentication,
                                  @RequestParam Map<String, String> params,
                                  Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }
        if (eventService.isCompletedEvent(event)) {
            populatePurchaseModel(model, event, "Este evento ya fue completado.");
            return VIEW_EVENT_PURCHASE;
        }

        String cardError = validateCardDetails(params);
        if (cardError != null) {
            populatePurchaseModel(model, event, cardError);
            return VIEW_EVENT_PURCHASE;
        }

        Map<Integer, Integer> quantities = parseQuantities(params);
        String paymentMethodValue = params.get("paymentMethod");

        PurchasePreview preview = buildPurchasePreview(event, quantities);
        if (preview.error() != null) {
            populatePurchaseModel(model, event, preview.error());
            return VIEW_EVENT_PURCHASE;
        }

        String trackingId = UUID.randomUUID().toString();
        writePurchaseJson(authentication.getName(), params, preview, trackingId);

        PendingPaymentRequest pending = new PendingPaymentRequest(
                id,
                authentication.getName(),
                quantities,
                paymentMethodValue,
                params.get("cardBrand"),
                params.get("cardNumber"),
                params.get("cardCvv"),
                preview.totalTickets(),
                preview.totalToPay(),
            preview.ticketTypeBreakdown(),
            Instant.now()
        );
        paymentTrackingService.registerPending(trackingId, pending);
        paymentProcessingService.processPayment(trackingId, pending);

        model.addAttribute("event", event);
        model.addAttribute("trackingId", trackingId);
        return VIEW_PAYMENT_PROCESSING;
    }

    @GetMapping("/purchase/result/{trackingId}")
    public String showPurchaseResult(@PathVariable String trackingId, Model model) {
        var resultOpt = paymentTrackingService.getResult(trackingId);
        if (resultOpt.isEmpty()) {
            var pendingOpt = paymentTrackingService.getPending(trackingId);
            if (pendingOpt.isEmpty()) {
                return "redirect:/user";
            }
            PendingPaymentRequest pending = pendingOpt.get();
            if (isPendingExpired(pending)) {
                PaymentResult timeoutResult = buildTimeoutResult(pending);
                paymentTrackingService.complete(trackingId, timeoutResult, true);
                Event event = eventService.getEventById(timeoutResult.eventId());
                if (event == null) {
                    return "redirect:/user";
                }
                populatePaymentResult(model, event, timeoutResult);
                model.addAttribute("trackingId", trackingId);
                return VIEW_PAYMENT_SUCCESS;
            }
            Event event = eventService.getEventById(pending.eventId());
            if (event == null) {
                return "redirect:/user";
            }
            model.addAttribute("event", event);
            model.addAttribute("trackingId", trackingId);
            return VIEW_PAYMENT_PROCESSING;
        }

        PaymentResult result = resultOpt.get();
        Event event = eventService.getEventById(result.eventId());
        if (event == null) {
            return "redirect:/user";
        }
        populatePaymentResult(model, event, result);
        model.addAttribute("trackingId", trackingId);
        return VIEW_PAYMENT_SUCCESS;
    }

    @GetMapping("/purchase/result/{trackingId}/ready")
    public ResponseEntity<Map<String, Object>> isResultReady(@PathVariable String trackingId) {
        boolean ready = paymentTrackingService.isReady(trackingId);
        if (!ready) {
            paymentTrackingService.getPending(trackingId).ifPresent(pending -> {
                if (isPendingExpired(pending)) {
                    PaymentResult timeoutResult = buildTimeoutResult(pending);
                    paymentTrackingService.complete(trackingId, timeoutResult, true);
                }
            });
            ready = paymentTrackingService.isReady(trackingId);
        }
        return ResponseEntity.ok(Map.of("ready", ready));
    }

    private boolean isPendingExpired(PendingPaymentRequest pending) {
        if (pending == null || pending.createdAt() == null) {
            return false;
        }
        return Instant.now().isAfter(pending.createdAt().plus(CHECKEO_TIMEOUT));
    }

    private PaymentResult buildTimeoutResult(PendingPaymentRequest pending) {
        return new PaymentResult(
                false,
                pending.eventId(),
                "error",
                "No fue posible contactar Checkeo. Intenta de nuevo.",
                null,
                pending.totalTickets(),
                PaymentMethod.fromValue(pending.paymentMethodValue()).getLabel(),
                pending.ticketTypeBreakdown(),
                Math.round(pending.totalToPay())
        );
    }

    private void populatePurchaseModel(Model model, Event event, String purchaseError) {
        model.addAttribute("event", event);
        model.addAttribute("ticketOptions", buildTicketOptions(event));
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        if (purchaseError != null && !purchaseError.isBlank()) {
            model.addAttribute("purchaseError", purchaseError);
        }
    }

    private void populatePaymentResult(Model model, Event event, PaymentResult result) {
        model.addAttribute("event", event);
        model.addAttribute("checkeoStatus", result.checkeoStatus());
        model.addAttribute("checkeoMessage", result.checkeoMessage());
        model.addAttribute("tickets", result.tickets());
        model.addAttribute("paymentMethod", result.paymentMethodLabel());
        model.addAttribute("ticketTypeBreakdown", result.ticketTypeBreakdown());
        model.addAttribute("total", result.total());
    }

    private List<TicketOption> buildTicketOptions(Event event) {
        if (event == null || event.getId() == null) {
            return List.of();
        }

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(event.getId());
        List<TicketOption> options = new ArrayList<>();
        for (EventTicketType assignment : assignments) {
            if (assignment == null || assignment.getTicketType() == null || assignment.getTicketType().getId() == null) {
                continue;
            }
            options.add(new TicketOption(
                    assignment.getTicketType().getId(),
                    resolveTicketTypeName(assignment),
                    resolveAvailableQuantity(assignment),
                    resolveTicketPrice(assignment, event)
            ));
        }
        return options;
    }

    private Map<Integer, Integer> parseQuantities(Map<String, String> params) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            return quantities;
        }

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("qty_")) {
                continue;
            }
            Integer ticketTypeId = parseTicketTypeId(key.substring(4));
            Integer quantity = parseQuantity(entry.getValue());
            if (ticketTypeId != null && quantity != null && quantity > 0) {
                quantities.put(ticketTypeId, quantity);
            }
        }
        return quantities;
    }

    private Integer parseTicketTypeId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(rawId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseQuantity(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveTicketTypeName(EventTicketType assignment) {
        return assignment.getTicketType() != null && assignment.getTicketType().getName() != null
                ? assignment.getTicketType().getName()
                : "Boleto";
    }

    private int resolveAvailableQuantity(EventTicketType assignment) {
        return assignment.getAvailableQuantity() != null ? assignment.getAvailableQuantity() : 0;
    }

    private double resolveTicketPrice(EventTicketType assignment, Event event) {
        if (assignment.getTicketPrice() != null && assignment.getTicketPrice() > 0) {
            return assignment.getTicketPrice();
        }
        Double eventPrice = event.getPrice();
        return eventPrice != null ? eventPrice : 0.0;
    }

    private PurchasePreview buildPurchasePreview(Event event, Map<Integer, Integer> quantities) {
        if (quantities == null || quantities.isEmpty()) {
            return PurchasePreview.error("Selecciona al menos un boleto para continuar.");
        }

        List<EventTicketType> assignments = eventTicketTypeRepository.findByEvent_Id(event.getId());
        Map<Integer, EventTicketType> assignmentByType = new LinkedHashMap<>();
        for (EventTicketType assignment : assignments) {
            if (assignment != null && assignment.getTicketType() != null && assignment.getTicketType().getId() != null) {
                assignmentByType.put(assignment.getTicketType().getId(), assignment);
            }
        }

        int totalTickets = 0;
        double totalToPay = 0.0;
        Map<String, Integer> breakdown = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : quantities.entrySet()) {
            Integer ticketTypeId = entry.getKey();
            Integer quantity = entry.getValue();
            if (quantity == null || quantity <= 0) {
                continue;
            }

            EventTicketType assignment = assignmentByType.get(ticketTypeId);
            if (assignment == null || assignment.getAvailableQuantity() == null) {
                return PurchasePreview.error("Uno de los tipos de boleto no es valido.");
            }
            if (quantity > assignment.getAvailableQuantity()) {
                return PurchasePreview.error("Una de las cantidades seleccionadas supera los boletos disponibles.");
            }

            double price = resolveTicketPrice(assignment, event);
            totalTickets += quantity;
            totalToPay += quantity * price;
            breakdown.merge(resolveTicketTypeName(assignment), quantity, Integer::sum);
        }

        if (totalTickets <= 0) {
            return PurchasePreview.error("Selecciona al menos un boleto para continuar.");
        }

        return new PurchasePreview(totalTickets, totalToPay, breakdown, null);
    }

    private void writePurchaseJson(String username, Map<String, String> formValues, PurchasePreview preview, String trackingId) {
        try {
            Path docsPath = Paths.get("docs");
            Files.createDirectories(docsPath);

            String cardBrand = cardBrandLabel(formValues.get("cardBrand"));
            String cardNumber = safeString(formValues.get("cardNumber"));
                String cardCsv = safeString(formValues.get("cardCvv"));
            String totalValue = formatAmount(preview.totalToPay());
            String timestamp = LocalDateTime.now().format(PURCHASE_JSON_DATE);
            Path outputFile = docsPath.resolve("purchase_" + timestamp + ".json");

                boolean isNu = "NU".equalsIgnoreCase(safeString(formValues.get("cardBrand")));
                String json = "{\n"
                    + "  \"usuario\": \"" + escapeJson(username) + "\",\n"
                    + "  \"tipo_tarjeta\": \"" + escapeJson(cardBrand) + "\",\n"
                    + "  \"numero_tarjeta\": \"" + escapeJson(cardNumber) + "\",\n"
                    + (isNu ? "  \"csv\": \"" + escapeJson(cardCsv) + "\",\n" : "")
                    + "  \"valor\": " + totalValue + ",\n"
                    + "  \"empresa_id\": 1,\n"
                    + "  \"tracking_id\": \"" + escapeJson(trackingId) + "\"\n"
                    + "}\n";

            Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private String formatAmount(double total) {
        BigDecimal value = BigDecimal.valueOf(total);
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cardBrandLabel(String cardBrand) {
        if (cardBrand == null) {
            return "";
        }
        String normalized = cardBrand.trim().toUpperCase();
        if ("VISA".equals(normalized)) {
            return "Visa";
        }
        if ("MASTERCARD".equals(normalized)) {
            return "Mastercard";
        }
        if ("NU".equals(normalized)) {
            return "nubank";
        }
        return cardBrand;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String validateCardDetails(Map<String, String> formValues) {
        String cardBrand = formValues.get("cardBrand");
        String cardNumber = formValues.get("cardNumber");

        if (!isValidCardBrand(cardBrand)) {
            return "Selecciona una marca de tarjeta valida.";
        }

        if (!isValidCardNumber(cardNumber)) {
            return "El numero de tarjeta no es valido.";
        }

        return null;
    }

    private boolean isValidCardBrand(String cardBrand) {
        if (cardBrand == null) {
            return false;
        }
        String normalized = cardBrand.trim().toUpperCase();
        return "VISA".equals(normalized) || "MASTERCARD".equals(normalized) || "NU".equals(normalized);
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        String digits = cardNumber.replaceAll("\\D", "");
        return digits.length() > 0 && digits.length() <= 19;
    }

    private record TicketOption(Integer ticketTypeId, String ticketTypeName, Integer availableQuantity, Double ticketPrice) {
    }

    private record PurchasePreview(int totalTickets, double totalToPay, Map<String, Integer> ticketTypeBreakdown,
                                   String error) {
        private static PurchasePreview error(String error) {
            return new PurchasePreview(0, 0.0, Map.of(), error);
        }
    }

}
