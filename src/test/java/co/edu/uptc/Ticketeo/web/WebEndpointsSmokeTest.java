package co.edu.uptc.Ticketeo.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import co.edu.uptc.Ticketeo.authentication.controllers.LoginController;
import co.edu.uptc.Ticketeo.authentication.controllers.RedirectController;
import co.edu.uptc.Ticketeo.authentication.controllers.RegisterController;
import co.edu.uptc.Ticketeo.configuration.GlobalModelAttributes;
import co.edu.uptc.Ticketeo.configuration.SecurityConfig;
import co.edu.uptc.Ticketeo.events.controllers.admin.AdminCategoryController;
import co.edu.uptc.Ticketeo.events.controllers.admin.AdminEventController;
import co.edu.uptc.Ticketeo.events.controllers.publicview.EventDetailsController;
import co.edu.uptc.Ticketeo.events.controllers.publicview.PublicViewHomeController;
import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.interest.controllers.AdminReportController;
import co.edu.uptc.Ticketeo.interest.services.InterestReportService;
import co.edu.uptc.Ticketeo.user.controllers.AdminUserController;
import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import co.edu.uptc.Ticketeo.user.services.UserService;

@WebMvcTest(controllers = {
        LoginController.class,
        RedirectController.class,
        RegisterController.class,
        PublicViewHomeController.class,
        EventDetailsController.class,
        AdminEventController.class,
        AdminCategoryController.class,
        AdminReportController.class,
        AdminUserController.class,
        GlobalModelAttributes.class
})

@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class WebEndpointsSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private EventCategoryService eventCategoryService;

    @MockitoBean
    private InterestReportService interestReportService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Configura datos mockeados para que todos los endpoints tengan respuestas válidas
        EventCategory category = EventCategory.builder().id(1).name("Music").color("#E74C3C").build();
        Event event = Event.builder()
                .id(1)
                .name("Rock Fest")
                .date(LocalDate.now().plusDays(5))
                .price(45000.0)
                .isActive(true)
                .category(category)
                .build();

        when(eventService.getEventsFiltered(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.getRandomEvents(anyInt())).thenReturn(List.of(event));
        when(eventService.getEventById(1)).thenReturn(event);
        when(eventService.getEventById(999)).thenReturn(null);
        when(eventService.getActiveEventsFiltered(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.getInactiveEventsFiltered(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.saveEvent(any(Event.class))).thenReturn(event);

        when(eventCategoryService.getAllCategories()).thenReturn(List.of(category));
        when(eventCategoryService.getEventCategoryById(1)).thenReturn(category);
        when(eventCategoryService.getCategoriesPaginated(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(category)));

        User user = User.builder().id(10L).username("user").password("pwd").role(Role.USER).build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(interestReportService.isUserInterested(1, 10L)).thenReturn(true);
        when(interestReportService.toggleInterest(any(Event.class), any(User.class))).thenReturn(true);
        when(interestReportService.getEventInterestRanking()).thenReturn(List.of());

        when(userService.getAllUsers(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(user)));

        doNothing().when(eventService).deactivateEvent(anyInt());
        doNothing().when(eventService).reactivateEvent(anyInt());
        doNothing().when(eventService).deleteEvent(anyInt());
        doNothing().when(eventCategoryService).deleteCategory(anyInt());
    }

    @Test
    void loginAndRegisterViews_shouldRender() throws Exception {
        // Verifica que las vistas de login y registro cargan correctamente sin errores.
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/login"));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/register"));
    }

    @Test
    void registerPost_withMismatchedPasswords_shouldReturnRegisterView() throws Exception {
        // Verifica que si las contraseñas no coinciden,
        // se retorna a la vista de registro con un error.
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "ana")
                        .param("password", "abc123")
                        .param("confirmPassword", "xyz999"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void registerPost_withValidData_shouldRedirectToLogin() throws Exception {
        // Verifica que un registro válido redirige al login con indicador de éxito.
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "ana")
                        .param("password", "abc123")
                        .param("confirmPassword", "abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    void rootAndPublicViews_shouldNotBreak() throws Exception {
        // Verifica navegación básica:
        // raíz redirige, home carga, detalle válido carga, detalle inexistente redirige con error.
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user"));

        mockMvc.perform(get("/user"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/userHome"));

        mockMvc.perform(get("/event/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/eventDetails"));

        mockMvc.perform(get("/event/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?error=notfound"));
    }

    @Test
    void eventInterestEndpoint_shouldRedirectBackToDetail() throws Exception {
        // Verifica que al marcar interés en un evento,
        // el endpoint redirige nuevamente al detalle del evento.
        mockMvc.perform(post("/event/1/interest")
                        .with(user("user").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/event/1"));
    }

        @Test
        void eventInterestEndpoint_ajaxRequest_shouldReturnJsonWithoutRedirect() throws Exception {
                // Verifica que el flujo AJAX no redirige y retorna el estado actualizado.
                mockMvc.perform(post("/event/1/interest")
                                                .with(user("user").roles("USER"))
                                                .with(csrf())
                                                .header("X-Requested-With", "XMLHttpRequest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.interested").value(true));
        }

    @Test
    void adminViews_shouldRenderWithoutServerErrors() throws Exception {
        // Verifica que todas las vistas del panel admin cargan correctamente con rol ADMIN.
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminEvents"));

        mockMvc.perform(get("/admin/inactive").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminInactiveEvents"));

        mockMvc.perform(get("/admin/event/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminEventForm"));

        mockMvc.perform(get("/admin/event/edit/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminEventForm"));

        mockMvc.perform(get("/admin/category").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminCategories"));

        mockMvc.perform(get("/admin/category/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminCategoryForm"));

        mockMvc.perform(get("/admin/category/edit/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("adminCategoryForm"));

        mockMvc.perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("interest/adminReportsMenu"));

        mockMvc.perform(get("/admin/reports/interest").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("interest/adminInterestReport"));

        mockMvc.perform(get("/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/adminUsers"));
    }

        @Test
        void userHome_forAdmin_shouldRedirectToAdminPanel() throws Exception {
                // Verifica que un ADMIN no pueda entrar a la vista de usuario.
                mockMvc.perform(get("/user").with(user("admin").roles("ADMIN")))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(redirectedUrl("/admin"));
        }

    @Test
    void adminMutationEndpoints_shouldRedirectAsExpected() throws Exception {
        // Verifica que las acciones administrativas (activar, eliminar, guardar, etc.)
        // ejecutan y redirigen correctamente.
        mockMvc.perform(get("/admin/event/deactivate/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        mockMvc.perform(get("/admin/event/activate/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/inactive"));

        mockMvc.perform(get("/admin/event/delete/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/inactive"));

        mockMvc.perform(post("/admin/category/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "Tech"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/category"));

        mockMvc.perform(get("/admin/category/delete/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/category"));

        mockMvc.perform(multipart("/admin/event/save")
                        .with(user("admin").roles("ADMIN"))
                        .file("imageFile", new byte[0])
                        .param("name", "Evento smoke")
                        .param("price", "15000")
                        .param("description", "prueba")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void adminEndpoints_forAnonymous_shouldRedirectToLogin() throws Exception {
        // Verifica que los endpoints de administración requieren autenticación
        // y redirigen si el usuario es anónimo.
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/category"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }
}
