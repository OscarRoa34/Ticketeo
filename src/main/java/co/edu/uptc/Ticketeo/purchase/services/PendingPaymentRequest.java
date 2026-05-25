package co.edu.uptc.Ticketeo.purchase.services;

import java.util.Map;

public record PendingPaymentRequest(
        Integer eventId,
        String username,
        Map<Integer, Integer> quantities,
        String paymentMethodValue,
        String cardBrand,
        String cardNumber,
        String cardCvv,
        int totalTickets,
        double totalToPay,
        Map<String, Integer> ticketTypeBreakdown
) {
}
