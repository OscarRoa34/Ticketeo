package co.edu.uptc.Ticketeo.purchase.services;

import java.util.Map;

public record PaymentResult(
        boolean success,
        Integer eventId,
        String checkeoStatus,
        String checkeoMessage,
        Long purchaseId,
        Integer tickets,
        String paymentMethodLabel,
        Map<String, Integer> ticketTypeBreakdown,
        Long total
) {
}
