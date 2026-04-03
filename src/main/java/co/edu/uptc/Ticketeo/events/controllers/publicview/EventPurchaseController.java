package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventPurchaseController {

    private static final Map<String, String> PAYMENT_METHOD_LABELS = Map.of(
            PaymentMethod.CARD.name(), PaymentMethod.CARD.getLabel(),
            PaymentMethod.PSE.name(), PaymentMethod.PSE.getLabel(),
            PaymentMethod.CASH.name(), PaymentMethod.CASH.getLabel()
    );

    private final EventService eventService;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PurchaseService purchaseService;
    private final UserRepository userRepository;

    @GetMapping("/{id}/purchase")
    public String showPurchasePage(@PathVariable Integer id, Model model, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }

        List<TicketOptionView> ticketOptions = loadTicketOptions(id, event);

        model.addAttribute("event", event);
        model.addAttribute("ticketOptions", ticketOptions);
        return "events/eventPurchase";
    }

    @Transactional
    @PostMapping("/{id}/purchase/pay")
    public String processPurchase(
            @PathVariable Integer id,
            @RequestParam Map<String, String> formValues,
            @RequestParam("paymentMethod") String paymentMethod,
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        Map<Integer, Integer> requestedQuantities = extractRequestedQuantities(formValues);
        PurchaseService.PurchaseCheckoutResult checkoutResult;
        try {
            checkoutResult = purchaseService.processPurchase(id, user.getUsername(), requestedQuantities, paymentMethod);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("purchaseError", ex.getMessage());
            return "redirect:/event/" + id + "/purchase";
        }

        redirectAttributes.addAttribute("tickets", checkoutResult.tickets());
        redirectAttributes.addAttribute("total", checkoutResult.total());
        redirectAttributes.addAttribute("paymentMethod", checkoutResult.paymentMethod());
        redirectAttributes.addFlashAttribute("ticketTypeBreakdown", checkoutResult.ticketTypeBreakdown());
        return "redirect:/event/" + id + "/purchase/success";
    }

    @GetMapping("/{id}/purchase/success")
    public String showPaymentSuccess(
            @PathVariable Integer id,
            @RequestParam("tickets") Integer tickets,
            @RequestParam("total") Long total,
            @RequestParam("paymentMethod") String paymentMethod,
            Model model,
            Authentication authentication
    ) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }

        model.addAttribute("event", event);
        model.addAttribute("tickets", tickets);
        model.addAttribute("total", total);
        model.addAttribute("paymentMethod", PAYMENT_METHOD_LABELS.getOrDefault(paymentMethod, "Tarjeta"));
        return "events/paymentSuccess";
    }

    private List<TicketOptionView> loadTicketOptions(Integer eventId, Event event) {
        return eventTicketTypeRepository.findByEvent_Id(eventId).stream()
                .filter(eventTicketType -> eventTicketType.getTicketType() != null && eventTicketType.getTicketType().getId() != null)
                .map(eventTicketType -> new TicketOptionView(
                        eventTicketType.getTicketType().getId(),
                        eventTicketType.getTicketType().getName(),
                        eventTicketType.getAvailableQuantity(),
                        resolveTicketPrice(eventTicketType, event)
                ))
                .toList();
    }

    private Map<Integer, Integer> extractRequestedQuantities(Map<String, String> formValues) {
        Map<Integer, Integer> requested = new LinkedHashMap<>();
        for (Map.Entry<String, String> formValue : formValues.entrySet()) {
            String key = formValue.getKey();
            if (!key.startsWith("qty_")) {
                continue;
            }

            try {
                Integer ticketTypeId = Integer.parseInt(key.substring(4));
                Integer quantity = Integer.parseInt(formValue.getValue());
                requested.put(ticketTypeId, quantity);
            } catch (NumberFormatException ignored) {
            }
        }
        return requested;
    }

    private double resolveUnitPrice(Event event) {
        return event.getPrice() == null ? 0.0 : event.getPrice();
    }

    private double resolveTicketPrice(EventTicketType eventTicketType, Event event) {
        if (eventTicketType.getTicketPrice() != null && eventTicketType.getTicketPrice() > 0) {
            return eventTicketType.getTicketPrice();
        }
        return resolveUnitPrice(event);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return PAYMENT_METHOD_LABELS.containsKey(paymentMethod) ? paymentMethod : "CARD";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }

    private record TicketOptionView(Integer ticketTypeId, String ticketTypeName, Integer availableQuantity, Double ticketPrice) {
    }
}
