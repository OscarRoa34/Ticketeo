package co.edu.uptc.Ticketeo.purchase.services;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class PaymentTrackingService {

    private final Map<String, PendingPaymentRequest> pending = new ConcurrentHashMap<>();
    private final Map<String, PaymentResult> results = new ConcurrentHashMap<>();
    private final Map<String, Boolean> llmReady = new ConcurrentHashMap<>();

    public void registerPending(String trackingId, PendingPaymentRequest request) {
        pending.put(trackingId, request);
        llmReady.put(trackingId, false);
    }

    public Optional<PendingPaymentRequest> getPending(String trackingId) {
        return Optional.ofNullable(pending.get(trackingId));
    }

    public void complete(String trackingId, PaymentResult result, boolean keepPending) {
        results.put(trackingId, result);
        if (!keepPending) {
            pending.remove(trackingId);
        }
    }

    public void markLlmReady(String trackingId) {
        llmReady.put(trackingId, true);
    }

    public Optional<PaymentResult> getResult(String trackingId) {
        return Optional.ofNullable(results.get(trackingId));
    }

    public boolean isReady(String trackingId) {
        return results.containsKey(trackingId) && Boolean.TRUE.equals(llmReady.get(trackingId));
    }
}