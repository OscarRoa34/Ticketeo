package co.edu.uptc.Ticketeo.events.controllers.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;

@ExtendWith(MockitoExtension.class)
class AdminCategoryControllerTest {

    @Mock
    private EventCategoryService eventCategoryService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AdminCategoryController adminCategoryController;

    @Test
    void saveCategory_returnsRedirectAndDelegatesToService() {
        EventCategory category = new EventCategory();

        String view = adminCategoryController.saveCategory(category, redirectAttributes);

        assertEquals("redirect:/admin/category", view);
        verify(eventCategoryService).saveCategory(category);
    }

    @Test
    void deleteCategory_ajaxSuccess_returnsOkResponse() {
        Object result = adminCategoryController.deleteCategory(1, "XMLHttpRequest", redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteCategory_nonAjaxFailure_returnsRedirectAndAddsFlashMessage() {
        doThrow(new IllegalStateException("No puedes eliminar una categoria que tiene eventos asociados."))
                .when(eventCategoryService)
                .deleteCategory(5);

        Object result = adminCategoryController.deleteCategory(5, null, redirectAttributes);

        assertEquals("redirect:/admin/category", result);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No puedes eliminar una categoria que tiene eventos asociados.");
    }

    @Test
    void deleteCategory_ajaxFailure_returnsConflictResponse() {
        doThrow(new IllegalStateException("No puedes eliminar una categoria que tiene eventos asociados."))
                .when(eventCategoryService)
                .deleteCategory(7);

        Object result = adminCategoryController.deleteCategory(7, "XMLHttpRequest", redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No puedes eliminar una categoria que tiene eventos asociados.", response.getBody());
    }
}

