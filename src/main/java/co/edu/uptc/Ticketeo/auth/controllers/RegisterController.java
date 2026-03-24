package co.edu.uptc.Ticketeo.auth.controllers;

import co.edu.uptc.Ticketeo.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String confirmPassword,
                               Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "register";
        }

        try {
            userService.registerNewUser(username, password);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
