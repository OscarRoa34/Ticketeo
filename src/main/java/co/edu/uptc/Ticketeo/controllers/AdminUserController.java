package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.User;
import co.edu.uptc.Ticketeo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final int PAGE_SIZE = 10;

    private final UserService userService;

    @GetMapping
    public String showUsers(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<User> userPage = userService.getAllUsers(page, PAGE_SIZE);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalUsers", userPage.getTotalElements());
        return "adminUsers";
    }
}

