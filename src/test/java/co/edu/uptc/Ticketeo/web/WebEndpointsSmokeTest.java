package co.edu.uptc.Ticketeo.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
import co.edu.uptc.Ticketeo.events.controllers.admin.AdminTicketTypeController;
import co.edu.uptc.Ticketeo.events.controllers.publicview.EventDetailsController;
import co.edu.uptc.Ticketeo.events.controllers.publicview.PublicViewHomeController;
import co.edu.uptc.Ticketeo.events.controllers.publicview.EventPurchaseController;
import co.edu.uptc.Ticketeo.purchase.controllers.UserPurchaseController;
import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.models.EventCategory;
import co.edu.uptc.Ticketeo.events.models.TicketType;
import co.edu.uptc.Ticketeo.events.models.EventTicketType;
import co.edu.uptc.Ticketeo.events.repositories.EventTicketTypeRepository;
import co.edu.uptc.Ticketeo.purchase.models.PaymentMethod;
import co.edu.uptc.Ticketeo.purchase.models.Purchase;
import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.purchase.services.PurchaseService;
import co.edu.uptc.Ticketeo.purchase.services.TicketPdfService;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;
import co.edu.uptc.Ticketeo.events.services.TicketTypeService;
import co.edu.uptc.Ticketeo.reports.controllers.AdminReportController;
import co.edu.uptc.Ticketeo.reports.services.InterestReportService;
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
        EventPurchaseController.class,
        UserPurchaseController.class,
        AdminEventController.class,
        AdminCategoryController.class,
        AdminTicketTypeController.class,
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
        private TicketTypeService ticketTypeService;

    @MockitoBean
    private InterestReportService interestReportService;

    @MockitoBean
    private EventTicketTypeRepository eventTicketTypeRepository;

    @MockitoBean
    private PurchaseService purchaseService;

    @MockitoBean
    private TicketPdfService ticketPdfService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Configura datos mockeados para que todos los endpoints tengan respuestas válidas
        EventCategory category = EventCategory.builder().id(1).name("Music").color("#E74C3C").build();
        TicketType ticketType = TicketType.builder().id(1).name("VIP").build();
        Event event = Event.builder()
                .id(1)
                .name("Rock Fest")
                .date(LocalDate.now().plusDays(5))
                .price(45000.0)
                .isActive(true)
                .category(category)
                .build();

        Event completedEvent = Event.builder()
                .id(2)
                .name("Old Fest")
                .date(LocalDate.now().minusDays(2))
                .price(45000.0)
                .isActive(true)
                .category(category)
                .build();

        EventTicketType eventTicketType = EventTicketType.builder()
                .id(1)
                .event(event)
                .ticketType(ticketType)
                .availableQuantity(100)
                .ticketPrice(45000.0)
                .build();

        when(eventService.getEventsFiltered(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.getRandomEvents(anyInt())).thenReturn(List.of(event));
        when(eventService.getEventById(1)).thenReturn(event);
        when(eventService.getEventById(2)).thenReturn(completedEvent);
        when(eventService.getEventById(999)).thenReturn(null);
        lenient().when(eventService.hasAvailableTicketsForEvent(1)).thenReturn(true);
        lenient().when(eventService.hasAvailableTicketsForEvent(2)).thenReturn(false);
        lenient().when(eventService.isCompletedEvent(any(Event.class))).thenAnswer(invocation -> {
            Event candidate = invocation.getArgument(0);
            return candidate != null && candidate.getDate() != null && candidate.getDate().isBefore(LocalDate.now());
        });
        when(eventService.getActiveEventsFiltered(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.getCompletedEventsFiltered(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.getInactiveEventsFiltered(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(eventService.saveEvent(any(Event.class))).thenReturn(event);
        when(eventTicketTypeRepository.findByEvent_Id(1)).thenReturn(List.of(eventTicketType));
        when(eventTicketTypeRepository.findByEvent_Id(999)).thenReturn(List.of());

        when(eventCategoryService.getAllCategories()).thenReturn(List.of(category));
        when(eventCategoryService.getEventCategoryById(1)).thenReturn(category);
        when(eventCategoryService.getCategoriesPaginated(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(category)));

        when(ticketTypeService.getAllTicketTypes()).thenReturn(List.of(ticketType));
        when(ticketTypeService.getTicketTypeById(1)).thenReturn(ticketType);
        when(ticketTypeService.getTicketTypesPaginated(anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(ticketType)));

        User user = User.builder().id(10L).username("user").password("pwd").role(Role.USER).build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(interestReportService.isUserInterested(1, 10L)).thenReturn(true);
        when(interestReportService.toggleInterest(any(Event.class), any(User.class))).thenReturn(true);
        when(interestReportService.getEventInterestRanking()).thenReturn(List.of());

        when(userService.getAllUsers(anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of(user)));

        Purchase purchase = Purchase.builder()
                .id(1L)
                .user(user)
                .eventId(1)
                .eventName("Rock Fest")
                .eventDate(event.getDate())
                .paymentMethod(PaymentMethod.CARD)
                .purchaseDate(java.time.LocalDateTime.now())
                .totalPaid(90000.0)
                .totalTickets(2)
                .build();
        PurchasedTicket purchasedTicket = PurchasedTicket.builder()
                .id(1L)
                .purchase(purchase)
                .ticketTypeName("VIP")
                .unitPrice(45000.0)
                .qrCode("TK-1-1-ABCD1234")
                .build();
        purchase.setTickets(List.of(purchasedTicket));

        when(purchaseService.getUserPurchases("user")).thenReturn(List.of(purchase));
        when(purchaseService.getUserPurchase(1L, "user")).thenReturn(purchase);
        when(purchaseService.getUserTicket(1L, "user")).thenReturn(purchasedTicket);
        when(purchaseService.processPurchase(anyInt(), any(), any(), any()))
                .thenReturn(new PurchaseService.PurchaseCheckoutResult(1L, 2, 90000L, "CARD", java.util.Map.of("VIP", 2)));

        when(ticketPdfService.generatePurchaseTicketsPdf(any(Purchase.class))).thenReturn("pdf-purchase".getBytes());
        when(ticketPdfService.generateSingleTicketPdf(any(PurchasedTicket.class))).thenReturn("pdf-ticket".getBytes());

        doNothing().when(eventService).deactivateEvent(anyInt());
        doNothing().when(eventService).reactivateEvent(anyInt());
        doNothing().when(eventService).deleteEvent(anyInt());
        doNothing().when(eventCategoryService).deleteCategory(anyInt());
        doNothing().when(ticketTypeService).deleteTicketType(anyInt());
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

        mockMvc.perform(get("/event/1/purchase").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/eventPurchase"));

        mockMvc.perform(get("/event/1/purchase/success")
                        .with(user("user").roles("USER"))
                        .param("tickets", "2")
                        .param("total", "90000")
                        .param("paymentMethod", "CARD"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/paymentSuccess"));

        mockMvc.perform(get("/user/purchases").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("user/userPurchases"));

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
                .andExpect(view().name("events/adminEvents"));

        mockMvc.perform(get("/admin/completed").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminCompletedEvents"));

        mockMvc.perform(get("/admin/inactive").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminInactiveEvents"));

        mockMvc.perform(get("/admin/event/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminEventForm"));

        mockMvc.perform(get("/admin/event/edit/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminEventForm"));

        mockMvc.perform(get("/admin/category").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminCategories"));

        mockMvc.perform(get("/admin/category/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminCategoryForm"));

        mockMvc.perform(get("/admin/category/edit/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminCategoryForm"));

        mockMvc.perform(get("/admin/ticket-type").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminTicketTypes"));

        mockMvc.perform(get("/admin/ticket-type/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminTicketTypeForm"));

        mockMvc.perform(get("/admin/ticket-type/edit/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("events/adminTicketTypeForm"));

        mockMvc.perform(get("/admin/reports").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/adminReportsMenu"));

        mockMvc.perform(get("/admin/reports/interest").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/adminInterestReport"));

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
        // Verifica que las acciones administrativas disponibles
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

        mockMvc.perform(post("/admin/ticket-type/save")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("name", "General"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ticket-type"));

        mockMvc.perform(get("/admin/ticket-type/delete/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/ticket-type"));

        mockMvc.perform(multipart("/admin/event/save")
                        .with(user("admin").roles("ADMIN"))
                        .file("imageFile", new byte[0])
                        .param("name", "Evento smoke")
                        .param("price", "15000")
                        .param("description", "prueba")
                        .param("ticketTypeIds", "1")
                        .param("ticketQuantity_1", "50")
                        .param("ticketPrice_1", "15000")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void deactivateEvent_withSoldTickets_shouldReturnAdminErrorFlow() throws Exception {
        doThrow(new IllegalArgumentException("No se puede desactivar un evento que ya tiene boletas vendidas."))
                .when(eventService).deactivateEvent(1);

        mockMvc.perform(get("/admin/event/deactivate/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?deactivateError=true"));

        mockMvc.perform(get("/admin/event/deactivate/1")
                        .with(user("admin").roles("ADMIN"))
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isConflict());
    }

    @Test
    void purchaseFlowEndpoints_shouldRedirectAsExpected() throws Exception {
        mockMvc.perform(get("/event/1/purchase"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(post("/event/1/purchase/pay")
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .param("paymentMethod", "CARD")
                        .param("qty_1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/event/1/purchase/success?tickets=2&total=90000&paymentMethod=CARD"));

        mockMvc.perform(get("/user/purchases/1/print")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/user/tickets/1/print")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void completedEvent_shouldBeReadOnlyForUserActions() throws Exception {
        mockMvc.perform(get("/event/2").with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isCompletedEvent", true));

        mockMvc.perform(get("/event/2/purchase").with(user("user").roles("USER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/event/2"));

        mockMvc.perform(post("/event/2/interest")
                        .with(user("user").roles("USER"))
                        .with(csrf())
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

}
