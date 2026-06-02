package co.edu.uptc.Ticketeo.purchase.messaging;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import co.edu.uptc.Ticketeo.config.RabbitMQConfig;
import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.services.PaymentResult;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventService eventService;

    public void publish(String trackingId, PaymentResult result) {
        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("trackingId", trackingId);
        mensaje.put("status", result.checkeoStatus());
        mensaje.put("message", result.checkeoMessage());

        Map<String, Object> detail = new HashMap<>();
        detail.put("purchaseId", result.purchaseId());
        detail.put("tickets", result.tickets());
        detail.put("paymentMethod", result.paymentMethodLabel());
        mensaje.put("detail", detail);

        Map<String, Object> eventInfo = buildEventInfo(result.eventId());
        mensaje.put("event", eventInfo);

        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTOS_PAGO_QUEUE, mensaje);
    }

    private Map<String, Object> buildEventInfo(Integer eventId) {
        Map<String, Object> eventInfo = new HashMap<>();
        if (eventId == null) {
            return eventInfo;
        }
        Event event = eventService.getEventById(eventId);
        if (event == null) {
            return eventInfo;
        }
        eventInfo.put("nombre", event.getName());
        eventInfo.put("fecha", event.getDate() != null ? event.getDate().toString() : "");
        eventInfo.put("categoria", event.getCategory() != null ? event.getCategory().getName() : "");
        eventInfo.put("precio", event.getPrice() != null ? event.getPrice() : 0.0);
        return eventInfo;
    }
}
