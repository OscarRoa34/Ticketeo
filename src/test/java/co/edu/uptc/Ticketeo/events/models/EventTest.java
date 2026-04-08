package co.edu.uptc.Ticketeo.events.models;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        Event event = new Event();

        event.onCreate();

        assertNotNull(event.getCreatedAt());
    }

    @Test
    void onCreate_keepsCreatedAtWhenAlreadyDefined() {
        Event event = new Event();
        LocalDateTime fixedDate = LocalDateTime.of(2026, 4, 8, 10, 15, 0);
        event.setCreatedAt(fixedDate);

        event.onCreate();

        assertSame(fixedDate, event.getCreatedAt());
    }
}
