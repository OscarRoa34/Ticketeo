package co.edu.uptc.Ticketeo.user.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.user.models.DocumentType;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.services.UserService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final EventCategoryService eventCategoryService;

    @GetMapping
    public String showProfile(Authentication authentication,
                              @RequestParam(value = "returnUrl", required = false) String returnUrl,
                              Model model) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("profile", UserProfileForm.fromUser(user));
        model.addAttribute("documentTypes", DocumentType.values());
        model.addAttribute("categories", eventCategoryService.getAllCategories());
        model.addAttribute("returnUrl", sanitizeReturnUrl(returnUrl));
        return "user/userProfile";
    }

    @PostMapping
    public String updateProfile(Authentication authentication,
                                @RequestParam("firstName") String firstName,
                                @RequestParam("lastName") String lastName,
                                @RequestParam("documentType") String documentType,
                                @RequestParam("documentNumber") String documentNumber,
                                @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (!isAuthenticated(authentication)) {
            return "redirect:/login";
        }

        User user = userService.getByUsername(authentication.getName());
        if (user == null) {
            return "redirect:/login";
        }

        DocumentType parsedType;
        try {
            parsedType = DocumentType.valueOf(documentType);
        } catch (IllegalArgumentException ex) {
            parsedType = null;
        }

        if (isBlank(firstName) || isBlank(lastName) || parsedType == null || isBlank(documentNumber)) {
            model.addAttribute("errorMessage", "Completa todos los datos del perfil para continuar.");
            model.addAttribute("categories", eventCategoryService.getAllCategories());
            model.addAttribute("documentTypes", DocumentType.values());
            model.addAttribute("returnUrl", sanitizeReturnUrl(returnUrl));
            model.addAttribute("profile", new UserProfileForm(firstName, lastName, documentType, documentNumber));
            return "user/userProfile";
        }

        userService.updateProfile(user, firstName, lastName, parsedType, documentNumber);
        redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado correctamente.");

        String safeReturnUrl = sanitizeReturnUrl(returnUrl);
        if (safeReturnUrl != null) {
            return "redirect:" + safeReturnUrl;
        }
        return "redirect:/user";
    }

    private String sanitizeReturnUrl(String returnUrl) {
        if (isBlank(returnUrl)) {
            return null;
        }
        String trimmed = returnUrl.trim();
        if (!trimmed.startsWith("/") || trimmed.contains("://")) {
            return null;
        }
        return trimmed;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record UserProfileForm(String firstName, String lastName, String documentType, String documentNumber) {
        public static UserProfileForm fromUser(User user) {
            return new UserProfileForm(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDocumentType() == null ? "" : user.getDocumentType().name(),
                    user.getDocumentNumber()
            );
        }
    }
}

