package co.edu.uptc.Ticketeo.purchase.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.purchase.services.TicketPdfService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserPurchaseController {

    private final PurchaseService purchaseService;
    private final TicketPdfService ticketPdfService;
    private final EventCategoryService eventCategoryService;

    @GetMapping("/purchases")
    public String showUserPurchases(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<Purchase> purchases = purchaseService.getUserPurchases(username);

        model.addAttribute("purchases", purchases);
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        return "user/userPurchases";
    }

    @GetMapping("/purchases/{purchaseId}/print")
    public ResponseEntity<byte[]> printPurchase(@PathVariable Long purchaseId, Authentication authentication) {
        String username = authentication.getName();
        Purchase purchase = purchaseService.getUserPurchase(purchaseId, username);
        if (purchase == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdf = ticketPdfService.generatePurchaseTicketsPdf(purchase);
        return buildPdfResponse(pdf, ticketPdfService.generatePurchaseFilename(purchase));
    }

    @GetMapping("/tickets/{ticketId}/print")
    public ResponseEntity<byte[]> printSingleTicket(@PathVariable Long ticketId, Authentication authentication) {
        String username = authentication.getName();
        PurchasedTicket ticket = purchaseService.getUserTicket(ticketId, username);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdf = ticketPdfService.generateSingleTicketPdf(ticket);
        return buildPdfResponse(pdf, ticketPdfService.generateSingleTicketFilename(ticket));
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

