package co.edu.uptc.Ticketeo.purchase.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PurchaseLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("PURCHASE_TRACKER");
    
    // Instanciación directa: Es thread-safe y nos independiza del ciclo de vida de Spring
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void log(String nivel, String mensaje, Map<String, Object> extra) {
        try {
            Map<String, Object> entrada = new LinkedHashMap<>();
            entrada.put("timestamp", Instant.now().toString());
            entrada.put("servicio", "checkeo");
            entrada.put("nivel", nivel.toUpperCase());
            entrada.put("mensaje", mensaje);

            // Une los mapas dinámicamente al mismo nivel raíz
            if (extra != null) {
                entrada.putAll(extra);
            }

            String jsonLine = objectMapper.writeValueAsString(entrada);

            switch (nivel.toUpperCase()) {
                case "ERROR" -> LOGGER.error(jsonLine);
                case "WARN" -> LOGGER.warn(jsonLine);
                default -> LOGGER.info(jsonLine);
            }
        } catch (Exception e) {
            LOGGER.error("{\"servicio\":\"checkeo\",\"nivel\":\"ERROR\",\"mensaje\":\"Error serializando log JSON\"}");
        }
    }
}