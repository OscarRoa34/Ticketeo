package co.edu.uptc.Ticketeo.purchase.controllers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.purchase.services.TicketPdfService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserPurchaseController {

    private static final String STATUS_COMPLETED = "Completado";
    private static final String STATUS_ACTIVE = "Activo";
    private static final String VIEW_USER_PURCHASES = "user/userPurchases";

    private final PurchaseService purchaseService;
    private final TicketPdfService ticketPdfService;
    private final EventCategoryService eventCategoryService;
    private final EventService eventService;

    @GetMapping("/purchases")
    public String showUserPurchases(Authentication authentication, Model model) {
        String username = resolveUsername(authentication);
        List<Purchase> purchases = purchaseService.getUserPurchases(username);
        Map<Long, String> purchaseEventStatus = buildPurchaseEventStatus(purchases);
        populatePurchasesModel(model, purchases, purchaseEventStatus);
        return VIEW_USER_PURCHASES;
    }

    private void populatePurchasesModel(Model model, List<Purchase> purchases,
                                        Map<Long, String> purchaseEventStatus) {
        model.addAttribute("purchases", purchases);
        model.addAttribute("purchaseEventStatus", purchaseEventStatus);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
    }

    private Map<Long, String> buildPurchaseEventStatus(List<Purchase> purchases) {
        Map<Long, String> purchaseEventStatus = new HashMap<>();
        for (Purchase purchase : purchases) {
            purchaseEventStatus.put(purchase.getId(), resolvePurchaseStatus(purchase));
        }
        return purchaseEventStatus;
    }

    private String resolvePurchaseStatus(Purchase purchase) {
        LocalDate eventDate = resolveEventDate(purchase);
        return isCompletedEvent(eventDate) ? STATUS_COMPLETED : STATUS_ACTIVE;
    }

    private boolean isCompletedEvent(LocalDate eventDate) {
        return eventDate != null && eventDate.isBefore(LocalDate.now());
    }

    private LocalDate resolveEventDate(Purchase purchase) {
        LocalDate currentEventDate = resolveCurrentEventDate(purchase.getEventId());
        return currentEventDate != null ? currentEventDate : purchase.getEventDate();
    }

    private LocalDate resolveCurrentEventDate(Integer eventId) {
        if (eventId == null) {
            return null;
        }
        var event = eventService.getEventById(eventId);
        return event != null ? event.getDate() : null;
    }

    @GetMapping("/purchases/{purchaseId}/print")
    public ResponseEntity<byte[]> printPurchase(@PathVariable Long purchaseId, Authentication authentication) {
        String username = resolveUsername(authentication);
        Purchase purchase = purchaseService.getUserPurchase(purchaseId, username);
        return buildPrintablePdfResponse(purchase,
                ticketPdfService::generatePurchaseTicketsPdf,
                ticketPdfService::generatePurchaseFilename);
    }

    @GetMapping("/tickets/{ticketId}/print")
    public ResponseEntity<byte[]> printSingleTicket(@PathVariable Long ticketId, Authentication authentication) {
        String username = resolveUsername(authentication);
        PurchasedTicket ticket = purchaseService.getUserTicket(ticketId, username);
        return buildPrintablePdfResponse(ticket,
                ticketPdfService::generateSingleTicketPdf,
                ticketPdfService::generateSingleTicketFilename);
    }

    private String resolveUsername(Authentication authentication) {
        return authentication.getName();
    }

    private <T> ResponseEntity<byte[]> buildPrintablePdfResponse(T printable,
                                                                  Function<T, byte[]> pdfGenerator,
                                                                  Function<T, String> filenameGenerator) {
        if (printable == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] pdf = pdfGenerator.apply(printable);
        return buildPdfResponse(pdf, filenameGenerator.apply(printable));
    }

    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}

