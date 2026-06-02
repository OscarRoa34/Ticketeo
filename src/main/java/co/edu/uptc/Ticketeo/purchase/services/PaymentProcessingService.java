package co.edu.uptc.Ticketeo.purchase.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async; // Asegúrate de crear este paquete/clase
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.edu.uptc.Ticketeo.purchase.logging.PurchaseLogger;
import co.edu.uptc.Ticketeo.purchase.messaging.PaymentEventPublisher;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    @Value("${checkeo.service.url:http://localhost:8000}")
    private String checkeoBaseUrl;

    private final PurchaseService purchaseService;
    private final PaymentTrackingService trackingService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PurchaseLogger purchaseLogger; // <-- Inyección del componente de logs estructurados

    @Async
    public void processPayment(String trackingId, PendingPaymentRequest request) {
        // [FASE 1] CLICK EN PAGAR: Registro del inicio del proceso asíncrono
        Map<String, Object> extraInicio = Map.of(
            "trackingId", trackingId,
            "eventId", request.eventId(),
            "username", safeString(request.username()),
            "totalToPay", request.totalToPay(),
            "paymentMethod", request.paymentMethodValue()
        );
        purchaseLogger.log("INFO", "Click en pagar: Iniciando procesamiento asíncrono de la orden", extraInicio);

        CheckeoResult checkeoResult = sendPurchaseToCheckeo(request, trackingId);
        
        // Contexto compartido para el resultado de la pasarela
        Map<String, Object> extraCheckeo = Map.of(
            "trackingId", trackingId,
            "checkeoStatus", checkeoResult.status(),
            "checkeoMessage", checkeoResult.message()
        );

        if ("error".equals(checkeoResult.status())) {
            // [FASE 2B] ERROR EN PASARELA
            purchaseLogger.log("ERROR", "Proceso interrumpido: Pago rechazado o fallido en Checkeo API", extraCheckeo);

            PaymentResult result = new PaymentResult(
                    false,
                    request.eventId(),
                    "error",
                    checkeoResult.message(),
                    null,
                    request.totalTickets(),
                    PaymentMethod.fromValue(request.paymentMethodValue()).getLabel(),
                    request.ticketTypeBreakdown(),
                    Math.round(request.totalToPay())
            );
            trackingService.complete(trackingId, result, true);
            paymentEventPublisher.publish(trackingId, result);
            return;
        }

        // [FASE 2A] PASARELA APROBADA
        purchaseLogger.log("INFO", "Validación exitosa: Pago aprobado por Checkeo API", extraCheckeo);

        try {
            PurchaseService.PurchaseCheckoutResult purchase = purchaseService.processPurchase(
                    request.eventId(),
                    request.username(),
                    request.quantities(),
                    request.paymentMethodValue()
            );

            // [FASE 3A] PROCESO LOCAL EXITOSO
            Map<String, Object> extraExitoLocal = Map.of(
                "trackingId", trackingId,
                "purchaseId", purchase.purchaseId(),
                "totalTickets", purchase.tickets(),
                "totalPaid", purchase.total(),
                "breakdown", purchase.ticketTypeBreakdown()
            );
            purchaseLogger.log("INFO", "Transacción completada: Compra asentada y tickets generados en DB", extraExitoLocal);

            PaymentResult result = new PaymentResult(
                    true,
                    request.eventId(),
                    "success",
                    checkeoResult.message(),
                    purchase.purchaseId(),
                    purchase.tickets(),
                    PaymentMethod.fromValue(purchase.paymentMethod()).getLabel(),
                    purchase.ticketTypeBreakdown(),
                    purchase.total()
            );
            trackingService.complete(trackingId, result, false);
            paymentEventPublisher.publish(trackingId, result);

        } catch (IllegalArgumentException ex) {
            // [FASE 3B] ERROR DE NEGOCIO LOCAL (Ej: Se agotaron los boletos en el milisegundo intermedio)
            Map<String, Object> extraErrorLocal = Map.of(
                "trackingId", trackingId,
                "errorDetail", safeString(ex.getMessage()),
                "eventId", request.eventId()
            );
            purchaseLogger.log("ERROR", "Fallo de consistencia: No se pudo registrar la compra localmente", extraErrorLocal);

            PaymentResult result = new PaymentResult(
                    false,
                    request.eventId(),
                    "error",
                    ex.getMessage(),
                    null,
                    request.totalTickets(),
                    PaymentMethod.fromValue(request.paymentMethodValue()).getLabel(),
                    request.ticketTypeBreakdown(),
                    Math.round(request.totalToPay())
            );
            trackingService.complete(trackingId, result, true);
            paymentEventPublisher.publish(trackingId, result);
        }
    }

    private CheckeoResult sendPurchaseToCheckeo(PendingPaymentRequest request, String trackingId) {
        // Registro previo al envío de datos sensibles a la API externa
        purchaseLogger.log("INFO", "Conectando con pasarela externa: Enviando POST a /pagos", Map.of("trackingId", trackingId));

        try {
            Map<String, Object> payload = buildCheckeoPayload(request, trackingId);

            RestTemplate restTemplate = new RestTemplate();
            var response = restTemplate.postForEntity(checkeoBaseUrl + "/pagos", payload, String.class);
            String message = resolveCheckeoMessage(response.getBody(), "Pago aprobado por Checkeo.", false);
            return CheckeoResult.success(message);
        } catch (HttpStatusCodeException ex) {
            String fallback = ex.getStatusCode().value() == 402
                    ? "Pago rechazado por Checkeo."
                    : "No fue posible validar el pago en Checkeo.";
            String message = resolveCheckeoMessage(ex.getResponseBodyAsString(), fallback, true);
            return CheckeoResult.error(message);
        } catch (Exception ignored) {
            return CheckeoResult.error("No fue posible validar el pago en Checkeo.");
        }
    }

    private Map<String, Object> buildCheckeoPayload(PendingPaymentRequest request, String trackingId) {
        String cardBrand = cardBrandLabel(request.cardBrand());
        String cardNumber = safeString(request.cardNumber());
        String cardCsv = safeString(request.cardCvv());
        double totalValue = parseAmount(request.totalToPay());
        boolean isNu = "NU".equalsIgnoreCase(safeString(request.cardBrand()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("usuario", safeString(request.username()));
        payload.put("tipo_tarjeta", cardBrand);
        payload.put("numero_tarjeta", cardNumber);
        if (isNu) {
            payload.put("csv", cardCsv);
        }
        payload.put("valor", totalValue);
        payload.put("empresa_id", 1);
        payload.put("tracking_id", trackingId);
        return payload;
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

    private record CheckeoResult(String status, String message) {
        private static CheckeoResult success(String message) {
            return new CheckeoResult("success", message);
        }

        private static CheckeoResult error(String message) {
            return new CheckeoResult("error", message);
        }
    }
}