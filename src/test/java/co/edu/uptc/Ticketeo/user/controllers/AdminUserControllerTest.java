package co.edu.uptc.Ticketeo.user.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.ui.ExtendedModelMap;

import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.services.UserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void showUsers_loadsPaginatedUsersInModel() {
        User user = new User();
        user.setUsername("admin");
        Page<User> page = new PageImpl<>(List.of(user));
        when(userService.getAllUsers(2, 10)).thenReturn(page);

        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminUserController.showUsers(2, model);

        assertEquals("user/adminUsers", view);
        assertEquals(List.of(user), model.get("users"));
        assertEquals(2, model.get("currentPage"));
        assertEquals(page.getTotalPages(), model.get("totalPages"));
        assertEquals(page.getTotalElements(), model.get("totalUsers"));
    }
}

