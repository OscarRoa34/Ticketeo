package co.edu.uptc.Ticketeo.events.controllers.publicview;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import co.edu.uptc.Ticketeo.user.services.UserService;
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
        private static final DateTimeFormatter PURCHASE_JSON_DATE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final EventService eventService;
    private final EventTicketTypeRepository eventTicketTypeRepository;
    private final PurchaseService purchaseService;
    private final UserService userService;

    @GetMapping("/{id}/purchase")
    public String showPurchasePage(@PathVariable Integer id,
                                   Model model,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return "redirect:/?error=notfound";
        }
        if (eventService.isCompletedEvent(event)) {
            return "redirect:/event/" + id;
        }

        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }
        if (!userService.isProfileComplete(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completa tu perfil antes de comprar boletas.");
            return "redirect:/user/profile?returnUrl=/event/" + id + "/purchase";
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
            RedirectAttributes redirectAttributes,
            Authentication authentication
    ) {
        PurchaseContext context = resolvePurchaseContext(id, authentication, redirectAttributes);
        if (context.redirect() != null) {
            return context.redirect();
        }

        String cardError = validateCardDetails(formValues);
        if (cardError != null) {
            redirectAttributes.addFlashAttribute("purchaseError", cardError);
            return "redirect:/event/" + id + "/purchase";
        }

        return handleCheckout(context, formValues, redirectAttributes);
    }

    @GetMapping("/{id}/purchase/success")
    public String showPaymentSuccess(
            @PathVariable Integer id,
            @RequestParam Map<String, String> params,
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

        Integer tickets = parseIntegerParam(params, "tickets");
        Long total = parseLongParam(params, "total");
        String paymentMethod = normalizePaymentMethod(params.get("paymentMethod"));

        model.addAttribute("event", event);
        model.addAttribute("tickets", tickets == null ? 0 : tickets);
        model.addAttribute("total", total == null ? 0L : total);
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
                int ticketTypeId = Integer.parseInt(key.substring(4));
                int quantity = Integer.parseInt(formValue.getValue());
                requested.put(ticketTypeId, quantity);
            } catch (NumberFormatException ignored) {
            }
        }
        return requested;
    }

    private double resolveUnitPrice(Event event) {
        Double price = event.getPrice();
        return price == null ? 0.0 : price;
    }

    private double resolveTicketPrice(EventTicketType eventTicketType, Event event) {
        if (eventTicketType.getTicketPrice() != null && eventTicketType.getTicketPrice() > 0) {
            return eventTicketType.getTicketPrice();
        }
        return resolveUnitPrice(event);
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null) {
            return "CARD";
        }
        return PAYMENT_METHOD_LABELS.containsKey(paymentMethod) ? paymentMethod : "CARD";
    }

    private PurchaseContext resolvePurchaseContext(Integer id, Authentication authentication, RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(authentication)) {
            return PurchaseContext.redirect("redirect:/login");
        }

        Event event = eventService.getEventById(id);
        if (event == null) {
            return PurchaseContext.redirect("redirect:/?error=notfound");
        }
        if (eventService.isCompletedEvent(event)) {
            return PurchaseContext.redirect("redirect:/event/" + id);
        }

        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return PurchaseContext.redirect("redirect:/login");
        }
        if (!userService.isProfileComplete(user)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completa tu perfil antes de comprar boletas.");
            return PurchaseContext.redirect("redirect:/user/profile?returnUrl=/event/" + id + "/purchase");
        }

        return new PurchaseContext(event, user, null);
    }

    private String validateCardDetails(Map<String, String> formValues) {
        String cardBrand = formValues.get("cardBrand");
        String cardNumber = formValues.get("cardNumber");

        if (!isValidCardBrand(cardBrand)) {
            return "Selecciona Visa o Mastercard.";
        }

        if (!isValidCardNumber(cardNumber)) {
            return "El numero de tarjeta no es valido.";
        }

        return null;
    }

    private String handleCheckout(PurchaseContext context, Map<String, String> formValues, RedirectAttributes redirectAttributes) {
        Map<Integer, Integer> requestedQuantities = extractRequestedQuantities(formValues);
        String paymentMethod = normalizePaymentMethod(formValues.get("paymentMethod"));

        PurchaseService.PurchaseCheckoutResult checkoutResult;
        try {
            checkoutResult = purchaseService.processPurchase(
                    context.event().getId(),
                    context.user().getUsername(),
                    requestedQuantities,
                    paymentMethod
            );
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("purchaseError", ex.getMessage());
            return "redirect:/event/" + context.event().getId() + "/purchase";
        }

        redirectAttributes.addAttribute("tickets", checkoutResult.tickets());
        redirectAttributes.addAttribute("total", checkoutResult.total());
        redirectAttributes.addAttribute("paymentMethod", checkoutResult.paymentMethod());
        redirectAttributes.addFlashAttribute("ticketTypeBreakdown", checkoutResult.ticketTypeBreakdown());
        writePurchaseJson(context, formValues, checkoutResult);
        return "redirect:/event/" + context.event().getId() + "/purchase/success";
    }

    private void writePurchaseJson(PurchaseContext context, Map<String, String> formValues,
                                   PurchaseService.PurchaseCheckoutResult checkoutResult) {
        try {
            Path docsPath = Paths.get("docs");
            Files.createDirectories(docsPath);

            String cardBrand = cardBrandLabel(formValues.get("cardBrand"));
            String cardNumber = safeString(formValues.get("cardNumber"));
            String username = safeString(context.user().getUsername());
            String totalValue = formatAmount(checkoutResult.total());
            String timestamp = LocalDateTime.now().format(PURCHASE_JSON_DATE);
            Path outputFile = docsPath.resolve("purchase_" + timestamp + ".json");

            String json = "{\n"
                    + "  \"usuario\": \"" + escapeJson(username) + "\",\n"
                    + "  \"tipo_tarjeta\": \"" + escapeJson(cardBrand) + "\",\n"
                    + "  \"numero_tarjeta\": \"" + escapeJson(cardNumber) + "\",\n"
                    + "  \"valor\": " + totalValue + ",\n"
                    + "  \"empresa_id\": 1\n"
                    + "}\n";

            Files.writeString(outputFile, json, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    private String formatAmount(Long total) {
        BigDecimal value = total == null ? BigDecimal.ZERO : BigDecimal.valueOf(total);
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String cardBrandLabel(String cardBrand) {
        if (cardBrand == null) {
            return "";
        }
        String normalized = cardBrand.trim().toUpperCase();
        if ("VISA".equals(normalized)) {
            return "Visa";
        }
        if ("MASTERCARD".equals(normalized)) {
            return "Mastercard";
        }
        return cardBrand;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Integer parseIntegerParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseLongParam(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isValidCardBrand(String cardBrand) {
        if (cardBrand == null) {
            return false;
        }
        String normalized = cardBrand.trim().toUpperCase();
        return "VISA".equals(normalized) || "MASTERCARD".equals(normalized);
    }

    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }

        String digits = cardNumber.replaceAll("\\D", "");
        return digits.length() > 0 && digits.length() <= 19;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }

    private record PurchaseContext(Event event, User user, String redirect) {
        private static PurchaseContext redirect(String redirect) {
            return new PurchaseContext(null, null, redirect);
        }
    }

    private record TicketOptionView(Integer ticketTypeId, String ticketTypeName, Integer availableQuantity, Double ticketPrice) {
    }
}
