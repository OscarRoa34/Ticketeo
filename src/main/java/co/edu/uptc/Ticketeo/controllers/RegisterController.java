package co.edu.uptc.Ticketeo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.uptc.Ticketeo.services.UserService;
import lombok.RequiredArgsConstructor;

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
        
        // Validación básica de contraseñas iguales
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "register";
        }

        try {
            userService.registerNewUser(username, password);
            // Si el registro es exitoso, redirigimos al login con un mensaje de éxito
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            // Manejamos si el usuario ya existe
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
