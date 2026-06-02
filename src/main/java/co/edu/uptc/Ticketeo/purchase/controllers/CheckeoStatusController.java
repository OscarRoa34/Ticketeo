package co.edu.uptc.Ticketeo.purchase.controllers;

import java.time.Instant;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uptc.Ticketeo.purchase.logging.PurchaseLogger; // <-- Importación de tu nuevo logger
import co.edu.uptc.Ticketeo.purchase.websocket.PurchaseStatusUpdate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checkeo")
@RequiredArgsConstructor
public class CheckeoStatusController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckeoStatusController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final PurchaseLogger purchaseLogger; // <-- Inyección automática por Lombok

    @PostMapping("/purchases/{trackingId}/status")
    public ResponseEntity<Void> publishStatus(@PathVariable String trackingId,
                                              @RequestBody PurchaseStatusUpdate payload) {
        
        // Mantengo tu log tradicional por si tienes configurada la consola de desarrollo
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WS update recibido: trackingId={}, phase={}, status={}, message={}, detail={}",
                trackingId,
                payload.phase(),
                payload.status(),
                payload.message(),
                payload.detail());
        }

        // [FASE ACTUALIZACIÓN INTERMEDIA] Registro del Webhook en el log JSON plano
        Map<String, Object> extraStatus = Map.of(
            "trackingId", trackingId,
            "phase", safeString(payload.phase()),
            "status", safeString(payload.status()),
            "message", safeString(payload.message())
        );
        purchaseLogger.log("INFO", "Notificación de estado asíncrono recibida desde pasarela externa", extraStatus);

        // Lógica de negocio original para propagar por WebSockets
        PurchaseStatusUpdate update = new PurchaseStatusUpdate(
                trackingId,
                payload.phase(),
                payload.status(),
                payload.message(),
                payload.detail(),
                payload.timestamp() == null ? Instant.now() : payload.timestamp()
        );
        
        messagingTemplate.convertAndSend("/topic/purchases/" + trackingId, update);
        
        return ResponseEntity.accepted().build();
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}