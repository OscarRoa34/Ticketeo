package co.edu.uptc.Ticketeo.user.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.user.models.DocumentType;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.services.UserService;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private EventCategoryService eventCategoryService;

    @InjectMocks
    private UserProfileController userProfileController;

    @Test
    void showProfile_whenNotAuthenticated_redirectsToLogin() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = userProfileController.showProfile(null, "/user/purchases", model);

        assertEquals("redirect:/login", view);
    }

    @Test
    void showProfile_whenUserDoesNotExist_redirectsToLogin() {
        Authentication authentication = auth("jane", true);
        when(userService.getByUsername("jane")).thenReturn(null);

        String view = userProfileController.showProfile(authentication, "/user/purchases", new ExtendedModelMap());

        assertEquals("redirect:/login", view);
    }

    @Test
    void showProfile_whenValidUser_loadsProfileViewAndModel() {
        User user = new User();
        user.setFirstName("Ana");
        user.setLastName("Roa");
        user.setDocumentType(DocumentType.CC);
        user.setDocumentNumber("123");
        EventCategory category = new EventCategory();
        category.setId(7);
        category.setName("Conciertos");

        Authentication authentication = auth("ana", true);
        ExtendedModelMap model = new ExtendedModelMap();

        when(userService.getByUsername("ana")).thenReturn(user);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of(category));

        String view = userProfileController.showProfile(authentication, " /user/purchases ", model);

        assertEquals("user/userProfile", view);
        assertEquals("/user/purchases", model.get("returnUrl"));
        assertEquals(List.of(category), model.get("categories"));
        assertNotNull(model.get("profile"));
        assertNotNull(model.get("documentTypes"));
    }

    @Test
    void updateProfile_whenInvalidData_returnsProfileWithError() {
        User user = new User();
        Authentication authentication = auth("ana", true);
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(userService.getByUsername("ana")).thenReturn(user);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        String view = userProfileController.updateProfile(
                authentication,
                "Ana",
                "",
                "INVALID",
                "",
                "http://malicious.site",
                redirectAttributes,
                model);

        assertEquals("user/userProfile", view);
        assertEquals("Completa todos los datos del perfil para continuar.", model.get("errorMessage"));
        assertEquals(null, model.get("returnUrl"));
        verify(userService, never()).updateProfile(any(User.class), any(), any(), any(), any());
    }

    @Test
    void updateProfile_whenValidDataAndReturnUrl_redirectsToReturnUrl() {
        User user = new User();
        Authentication authentication = auth("ana", true);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(userService.getByUsername("ana")).thenReturn(user);

        String view = userProfileController.updateProfile(
                authentication,
                "Ana",
                "Roa",
                "CC",
                "1010",
                "/user/purchases",
                redirectAttributes,
                new ExtendedModelMap());

        assertEquals("redirect:/user/purchases", view);
        assertEquals("Perfil actualizado correctamente.", redirectAttributes.getFlashAttributes().get("successMessage"));
        verify(userService).updateProfile(eq(user), eq("Ana"), eq("Roa"), eq(DocumentType.CC), eq("1010"));
    }

    @Test
    void updateProfile_whenValidDataAndInvalidReturnUrl_redirectsToUserHome() {
        User user = new User();
        Authentication authentication = auth("ana", true);

        when(userService.getByUsername("ana")).thenReturn(user);

        String view = userProfileController.updateProfile(
                authentication,
                "Ana",
                "Roa",
                "PASSPORT",
                "AB123",
                "https://external-site",
                new RedirectAttributesModelMap(),
                new ExtendedModelMap());

        assertEquals("redirect:/user", view);
        verify(userService).updateProfile(eq(user), eq("Ana"), eq("Roa"), eq(DocumentType.PASSPORT), eq("AB123"));
    }

    private Authentication auth(String username, boolean authenticated) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        when(authentication.isAuthenticated()).thenReturn(authenticated);
        return authentication;
    }
}

