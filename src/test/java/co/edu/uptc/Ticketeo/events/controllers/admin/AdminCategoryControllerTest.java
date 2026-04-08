package co.edu.uptc.Ticketeo.events.controllers.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
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
        when(eventCategoryService.saveCategory(category)).thenReturn(category);

        String view = adminCategoryController.saveCategory(category, false, null, false, redirectAttributes);

        assertEquals("redirect:/admin/category", view);
        verify(eventCategoryService).saveCategory(category);
    }

    @Test
    void saveCategory_update_addsUpdatedFlashMessage() {
        EventCategory category = new EventCategory();
        category.setId(4);
        when(eventCategoryService.saveCategory(category)).thenReturn(category);

        String view = adminCategoryController.saveCategory(category, false, null, false, redirectAttributes);

        assertEquals("redirect:/admin/category", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Categoria actualizada correctamente.");
    }

    @Test
    void saveCategory_withEventFormContext_redirectsBackWithSelectedCategoryId() {
        EventCategory category = new EventCategory();
        category.setName("Música");
        EventCategory saved = new EventCategory();
        saved.setId(14);
        when(eventCategoryService.saveCategory(category)).thenReturn(saved);

        String view = adminCategoryController.saveCategory(category, true, null, false, redirectAttributes);

        assertEquals("redirect:/admin/event/new?selectedCategoryId=14", view);
    }

    @Test
    void saveCategory_withEventFormDraftContext_redirectsToDraftEventForm() {
        EventCategory category = new EventCategory();
        EventCategory saved = new EventCategory();
        saved.setId(9);
        when(eventCategoryService.saveCategory(category)).thenReturn(saved);

        String view = adminCategoryController.saveCategory(category, true, null, true, redirectAttributes);

        assertEquals("redirect:/admin/event/new?draft=true&selectedCategoryId=9", view);
    }

    @Test
    void saveCategory_withEventContextAndNullSavedEntity_redirectsWithoutSelectedId() {
        EventCategory category = new EventCategory();
        when(eventCategoryService.saveCategory(category)).thenReturn(null);

        String view = adminCategoryController.saveCategory(category, true, 10, false, redirectAttributes);

        assertEquals("redirect:/admin/event/edit/10", view);
    }

    @Test
    void showCreateForm_withEventFormContext_setsCancelPathToEventForm() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminCategoryController.showCreateForm(true, 7, false, model);

        assertEquals("events/adminCategoryForm", view);
        assertEquals("/admin/event/edit/7", model.get("cancelPath"));
    }

    @Test
    void showCreateForm_withoutEventContext_setsCancelPathToCategoryList() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = adminCategoryController.showCreateForm(false, null, false, model);

        assertEquals("events/adminCategoryForm", view);
        assertEquals("/admin/category", model.get("cancelPath"));
    }

    @Test
    void showCategories_returnsViewAndPaginationData() {
        ExtendedModelMap model = new ExtendedModelMap();
        Page<EventCategory> page = new PageImpl<>(java.util.List.of(new EventCategory()));
        when(eventCategoryService.getCategoriesPaginated(0, 6)).thenReturn(page);

        String view = adminCategoryController.showCategories(0, model);

        assertEquals("events/adminCategories", view);
        assertEquals(page.getContent(), model.get("categories"));
        assertEquals(0, model.get("currentPage"));
    }

    @Test
    void showEditForm_withEventContext_setsCategoryAndCancelPath() {
        ExtendedModelMap model = new ExtendedModelMap();
        EventCategory category = new EventCategory();
        category.setId(5);
        when(eventCategoryService.getEventCategoryById(5)).thenReturn(category);

        String view = adminCategoryController.showEditForm(5, true, 12, true, model);

        assertEquals("events/adminCategoryForm", view);
        assertEquals(category, model.get("category"));
        assertEquals("/admin/event/edit/12", model.get("cancelPath"));
    }

    @Test
    void deleteCategory_ajaxSuccess_returnsOkResponse() {
        Object result = adminCategoryController.deleteCategory(1, "XMLHttpRequest", redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteCategory_nonAjaxSuccess_returnsRedirectAndSuccessFlash() {
        Object result = adminCategoryController.deleteCategory(1, null, redirectAttributes);

        assertEquals("redirect:/admin/category", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Categoria eliminada correctamente.");
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

