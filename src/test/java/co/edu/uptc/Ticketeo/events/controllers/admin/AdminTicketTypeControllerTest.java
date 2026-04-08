package co.edu.uptc.Ticketeo.events.controllers.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.uptc.Ticketeo.events.controllers.admin.AdminTicketTypeController.TicketTypeForm;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;

@ExtendWith(MockitoExtension.class)
class AdminTicketTypeControllerTest {

    @Mock
    private TicketTypeService ticketTypeService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private Model model;

    @InjectMocks
    private AdminTicketTypeController adminTicketTypeController;

    @Test
    void saveTicketType_success_redirectsAndAddsSuccessFlash() {
        TicketTypeForm form = new TicketTypeForm();
        form.setName("VIP");

        String view = adminTicketTypeController.saveTicketType(form, redirectAttributes);

        assertEquals("redirect:/admin/ticket-type", view);
        verify(ticketTypeService).saveTicketType(any(TicketType.class));
        verify(redirectAttributes).addFlashAttribute("successMessage", "Tipo de ticket creado correctamente.");
    }

    @Test
    void saveTicketType_update_redirectsAndAddsUpdateFlash() {
        TicketTypeForm form = new TicketTypeForm();
        form.setId(9);
        form.setName("Platea");

        String view = adminTicketTypeController.saveTicketType(form, redirectAttributes);

        assertEquals("redirect:/admin/ticket-type", view);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Tipo de ticket actualizado correctamente.");
    }

    @Test
    void saveTicketType_serviceError_redirectsAndAddsErrorFlash() {
        TicketTypeForm form = new TicketTypeForm();
        form.setName("General");
        doThrow(new RuntimeException("boom")).when(ticketTypeService).saveTicketType(any(TicketType.class));

        String view = adminTicketTypeController.saveTicketType(form, redirectAttributes);

        assertEquals("redirect:/admin/ticket-type", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No fue posible guardar el tipo de ticket.");
    }

    @Test
    void deleteTicketType_ajaxSuccess_returnsOk() {
        Object result = adminTicketTypeController.deleteTicketType(1, "XMLHttpRequest", redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteTicketType_nonAjaxSuccess_redirectsAndAddsSuccessFlash() {
        Object result = adminTicketTypeController.deleteTicketType(11, null, redirectAttributes);

        assertEquals("redirect:/admin/ticket-type", result);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Tipo de ticket eliminado correctamente.");
    }

    @Test
    void deleteTicketType_nonAjaxFailure_redirectsWithErrorFlash() {
        doThrow(new RuntimeException("boom")).when(ticketTypeService).deleteTicketType(2);

        Object result = adminTicketTypeController.deleteTicketType(2, null, redirectAttributes);

        assertEquals("redirect:/admin/ticket-type", result);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "No fue posible eliminar el tipo de ticket.");
    }

    @Test
    void deleteTicketType_ajaxFailure_returnsConflict() {
        doThrow(new RuntimeException("boom")).when(ticketTypeService).deleteTicketType(3);

        Object result = adminTicketTypeController.deleteTicketType(3, "XMLHttpRequest", redirectAttributes);

        ResponseEntity<?> response = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("No fue posible eliminar el tipo de ticket.", response.getBody());
    }

    @Test
    void showTicketTypes_returnsConfiguredListViewAndModelData() {
        Page<TicketType> ticketTypePage = new PageImpl<>(List.of(new TicketType()));
        when(ticketTypeService.getTicketTypesPaginated(0, 6)).thenReturn(ticketTypePage);

        String view = adminTicketTypeController.showTicketTypes(0, model);

        assertEquals("events/adminTicketTypes", view);
        verify(model).addAttribute("ticketTypes", ticketTypePage.getContent());
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", ticketTypePage.getTotalPages());
    }

    @Test
    void showCreateForm_returnsFormViewWithEmptyForm() {
        String view = adminTicketTypeController.showCreateForm(model);

        assertEquals("events/adminTicketTypeForm", view);
        verify(model).addAttribute(any(String.class), any(TicketTypeForm.class));
    }

    @Test
    void showEditForm_mapsEntityToForm() {
        TicketType entity = new TicketType();
        entity.setId(5);
        entity.setName("General");
        when(ticketTypeService.getTicketTypeById(5)).thenReturn(entity);

        String view = adminTicketTypeController.showEditForm(5, model);

        assertEquals("events/adminTicketTypeForm", view);
        verify(model).addAttribute(any(String.class), any(TicketTypeForm.class));
    }

    @Test
    void ticketTypeForm_gettersAndSetters_work() {
        TicketTypeForm form = new TicketTypeForm();
        form.setId(21);
        form.setName("VIP");

        assertEquals(21, form.getId());
        assertEquals("VIP", form.getName());
        assertNotNull(form);
    }
}

