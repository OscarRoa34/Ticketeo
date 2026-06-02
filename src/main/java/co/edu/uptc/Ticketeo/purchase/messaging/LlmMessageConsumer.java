package co.edu.uptc.Ticketeo.purchase.messaging;

import java.util.Map;

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

    @RabbitListener(queues = RabbitMQConfig.MENSAJES_LLM_QUEUE)
    public void consume(Map<String, Object> mensaje) {
        String trackingId = (String) mensaje.get("trackingId");
        String status = (String) mensaje.get("status");
        String message = (String) mensaje.get("message");

        @SuppressWarnings("unchecked")
        Map<String, Object> detail = mensaje.containsKey("detail")
                ? (Map<String, Object>) mensaje.get("detail")
                : Map.of();

        PurchaseStatusUpdate update = new PurchaseStatusUpdate(
                trackingId,
                "resultado_final",
                status,
                message,
                detail,
                null
        );

        messagingTemplate.convertAndSend("/topic/purchases/" + trackingId, update);
    }
}
