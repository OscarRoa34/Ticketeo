package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventPurchaseController {

    private static final String VIEW_EVENT_PURCHASE = "events/eventPurchase";
    private static final String VIEW_PAYMENT_SUCCESS = "events/paymentSuccess";
    private static final DateTimeFormatter PURCHASE_JSON_DATE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    @Value("${checkeo.service.url:http://localhost:8000}")
    private String checkeoBaseUrl;

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PurchaseService purchaseService;

    @GetMapping("/{id}/purchase")
    public String showPurchaseForm(@PathVariable Integer id, Authentication authentication, Model model) {
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

        writePurchaseJson(authentication.getName(), params, preview);
        CheckeoResult checkeoResult = sendPurchaseToCheckeo(authentication.getName(), params, preview);
        if ("error".equals(checkeoResult.status())) {
            return populatePaymentFeedback(model, event, preview, paymentMethodValue, checkeoResult);
        }

        try {
            PurchaseService.PurchaseCheckoutResult result = purchaseService.processPurchase(
                    id,
                    authentication.getName(),
                    quantities,
                    paymentMethodValue
            );
            return populatePaymentSuccess(model, event, result, checkeoResult);
        } catch (IllegalArgumentException ex) {
            populatePurchaseModel(model, event, ex.getMessage());
            return VIEW_EVENT_PURCHASE;
        }
    }

    private void populatePurchaseModel(Model model, Event event, String purchaseError) {
        model.addAttribute("event", event);
        model.addAttribute("ticketOptions", buildTicketOptions(event));
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        if (purchaseError != null && !purchaseError.isBlank()) {
            model.addAttribute("purchaseError", purchaseError);
        }
    }

    private String populatePaymentSuccess(Model model, Event event, PurchaseService.PurchaseCheckoutResult result,
                                          CheckeoResult checkeoResult) {
        model.addAttribute("event", event);
        model.addAttribute("checkeoStatus", checkeoResult.status());
        model.addAttribute("checkeoMessage", checkeoResult.message());
        model.addAttribute("tickets", result.tickets());
        model.addAttribute("paymentMethod", PaymentMethod.fromValue(result.paymentMethod()).getLabel());
        model.addAttribute("ticketTypeBreakdown", result.ticketTypeBreakdown());
        model.addAttribute("total", result.total());
        return VIEW_PAYMENT_SUCCESS;
    }

    private String populatePaymentFeedback(Model model, Event event, PurchasePreview preview, String paymentMethodValue,
                                           CheckeoResult checkeoResult) {
        model.addAttribute("event", event);
        model.addAttribute("checkeoStatus", checkeoResult.status());
        model.addAttribute("checkeoMessage", checkeoResult.message());
        model.addAttribute("tickets", preview.totalTickets());
        model.addAttribute("paymentMethod", PaymentMethod.fromValue(paymentMethodValue).getLabel());
        model.addAttribute("ticketTypeBreakdown", preview.ticketTypeBreakdown());
        model.addAttribute("total", Math.round(preview.totalToPay()));
        return VIEW_PAYMENT_SUCCESS;
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

    private void writePurchaseJson(String username, Map<String, String> formValues, PurchasePreview preview) {
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
                    + "  \"empresa_id\": 1\n"
                    + "}\n";

            Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private CheckeoResult sendPurchaseToCheckeo(String username, Map<String, String> formValues, PurchasePreview preview) {
        try {
            Map<String, Object> payload = buildCheckeoPayload(username, formValues, preview);

            System.out.println("Checkeo payload: " + payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            RestTemplate restTemplate = new RestTemplate();
            var response = restTemplate.postForEntity(checkeoBaseUrl + "/pagos", request, String.class);
            System.out.println("Checkeo response status: " + response.getStatusCode().value());
            System.out.println("Checkeo response body: " + response.getBody());
            String message = resolveCheckeoMessage(response.getBody(), "Pago aprobado por Checkeo.", false);
            return CheckeoResult.success(message);
        } catch (HttpStatusCodeException ex) {
            System.out.println("Checkeo response status: " + ex.getStatusCode().value());
            System.out.println("Checkeo response body: " + ex.getResponseBodyAsString());
            String fallback = ex.getStatusCode().value() == 402
                    ? "Pago rechazado por Checkeo."
                    : "No fue posible validar el pago en Checkeo.";
            String message = resolveCheckeoMessage(ex.getResponseBodyAsString(), fallback, true);
            return CheckeoResult.error(message);
        } catch (Exception ignored) {
            return CheckeoResult.error("No fue posible validar el pago en Checkeo.");
        }
    }

    private String resolveCheckeoMessage(String body, String fallback, boolean errorPayload) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode detail = errorPayload ? root.path("detail") : root;
            String message = detail.path("message").asText(null);
            if (message == null || message.isBlank()) {
                return fallback;
            }
            String provider = detail.path("provider").asText(null);
            if (provider != null && !provider.isBlank()) {
                if (message.toUpperCase().contains(provider.toUpperCase())) {
                    return message;
                }
                return provider + ": " + message;
            }
            return message;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> buildCheckeoPayload(String username, Map<String, String> formValues,
                                                    PurchasePreview preview) {
        String cardBrand = cardBrandLabel(formValues.get("cardBrand"));
        String cardNumber = safeString(formValues.get("cardNumber"));
        String cardCsv = safeString(formValues.get("cardCvv"));
        double totalValue = parseAmount(preview.totalToPay());
        boolean isNu = "NU".equalsIgnoreCase(safeString(formValues.get("cardBrand")));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("usuario", safeString(username));
        payload.put("tipo_tarjeta", cardBrand);
        payload.put("numero_tarjeta", cardNumber);
        if (isNu) {
            payload.put("csv", cardCsv);
        }
        payload.put("valor", totalValue);
        payload.put("empresa_id", 1);
        return payload;
    }

    private String formatAmount(double total) {
        BigDecimal value = BigDecimal.valueOf(total);
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private double parseAmount(double total) {
        BigDecimal value = BigDecimal.valueOf(total);
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
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

    private record CheckeoResult(String status, String message) {
        private static CheckeoResult success(String message) {
            return new CheckeoResult("success", message);
        }

        private static CheckeoResult error(String message) {
            return new CheckeoResult("error", message);
        }
    }
}
