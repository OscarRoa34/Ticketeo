package co.edu.uptc.Ticketeo;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class PipelineFailureSmokeTest {

    @Test
    void shouldFailTemporarily() {
        fail("Intentional failure to validate CI pipeline behavior.");
    }
}

