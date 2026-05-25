package co.edu.uptc.Ticketeo.purchase.websocket;

import java.time.Instant;
import java.util.Map;

public record PurchaseStatusUpdate(
    String trackingId,
        String phase,
        String status,
        String message,
        Map<String, Object> detail,
        Instant timestamp
) {
    public PurchaseStatusUpdate {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
