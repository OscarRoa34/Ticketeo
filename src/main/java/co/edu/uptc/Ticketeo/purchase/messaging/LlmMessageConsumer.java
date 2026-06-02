package co.edu.uptc.Ticketeo.purchase.messaging;

import java.util.Map;

import co.edu.uptc.Ticketeo.purchase.services.PaymentResult;
import co.edu.uptc.Ticketeo.purchase.services.PaymentTrackingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import co.edu.uptc.Ticketeo.config.RabbitMQConfig;
import co.edu.uptc.Ticketeo.purchase.websocket.PurchaseStatusUpdate;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LlmMessageConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final PaymentTrackingService trackingService;

    @RabbitListener(queues = RabbitMQConfig.MENSAJES_LLM_QUEUE)
    public void consume(Map<String, Object> mensaje) {
        String trackingId = (String) mensaje.get("trackingId");
        String status = (String) mensaje.get("status");
        String message = (String) mensaje.get("message");

        @SuppressWarnings("unchecked")
        Map<String, Object> detail = mensaje.containsKey("detail")
                ? (Map<String, Object>) mensaje.get("detail")
                : Map.of();

        // Actualizar el mensaje en el resultado guardado
        trackingService.getResult(trackingId).ifPresent(result -> {
            PaymentResult updated = new PaymentResult(
                    result.success(),
                    result.eventId(),
                    result.checkeoStatus(),
                    message,
                    result.purchaseId(),
                    result.tickets(),
                    result.paymentMethodLabel(),
                    result.ticketTypeBreakdown(),
                    result.total()
            );
            trackingService.complete(trackingId, updated, !result.success());
        });


        PurchaseStatusUpdate update = new PurchaseStatusUpdate(
                trackingId,
                "resultado_final",
                status,
                message,
                detail,
                null
        );

        trackingService.markLlmReady(trackingId);
        
        messagingTemplate.convertAndSend("/topic/purchases/" + trackingId, update);
    }
}