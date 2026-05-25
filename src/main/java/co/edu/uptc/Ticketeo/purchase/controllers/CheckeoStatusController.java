package co.edu.uptc.Ticketeo.purchase.controllers;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uptc.Ticketeo.purchase.websocket.PurchaseStatusUpdate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/checkeo")
@RequiredArgsConstructor
public class CheckeoStatusController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckeoStatusController.class);
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/purchases/{trackingId}/status")
    public ResponseEntity<Void> publishStatus(@PathVariable String trackingId,
                                              @RequestBody PurchaseStatusUpdate payload) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("WS update recibido: trackingId={}, phase={}, status={}, message={}, detail={}",
                trackingId,
                payload.phase(),
                payload.status(),
                payload.message(),
                payload.detail());
        }
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
}
