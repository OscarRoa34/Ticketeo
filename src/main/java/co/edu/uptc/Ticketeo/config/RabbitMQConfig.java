package co.edu.uptc.Ticketeo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EVENTOS_PAGO_QUEUE = "eventos_pago";
    public static final String MENSAJES_LLM_QUEUE = "mensajes_llm";

    @Bean
    public Queue eventosPagoQueue() {
        return new Queue(EVENTOS_PAGO_QUEUE, true);
    }

    @Bean
    public Queue mensajesLlmQueue() {
        return new Queue(MENSAJES_LLM_QUEUE, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
