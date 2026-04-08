package co.edu.uptc.Ticketeo.events.controllers.publicview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.events.services.EventCategoryService;
import co.edu.uptc.Ticketeo.events.services.EventService;

@ExtendWith(MockitoExtension.class)
class PublicViewHomeControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private EventCategoryService eventCategoryService;

    @InjectMocks
    private PublicViewHomeController publicViewHomeController;

    @Test
    void showUserHome_whenAdminAuthenticated_redirectsToAdminDashboard() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        doReturn(authorities).when(auth).getAuthorities();

        String view = publicViewHomeController.showUserHome(0, null, null, "date_desc", auth, new ExtendedModelMap());

        assertEquals("redirect:/admin/dashboard", view);
        verify(eventService, never()).getEventsFiltered(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void showUserHome_whenRegularUser_buildsAvailabilityAndCompletedMaps() {
        Event listedOne = new Event();
        listedOne.setId(1);
        Event listedTwo = new Event();
        listedTwo.setId(2);

        Event carouselDuplicate = new Event();
        carouselDuplicate.setId(2);
        Event carouselUnique = new Event();
        carouselUnique.setId(3);
        Event carouselWithoutId = new Event();

        Page<Event> page = new PageImpl<>(List.of(listedOne, listedTwo));

        when(eventService.getEventsFiltered("rock", 7, 1, 6, "price_asc")).thenReturn(page);
        when(eventService.getRandomEvents(5)).thenReturn(List.of(carouselDuplicate, carouselUnique, carouselWithoutId));
        when(eventService.hasAvailableTicketsForEvent(1)).thenReturn(true);
        when(eventService.hasAvailableTicketsForEvent(2)).thenReturn(false);
        when(eventService.hasAvailableTicketsForEvent(3)).thenReturn(true);
        when(eventService.isCompletedEvent(listedOne)).thenReturn(false);
        when(eventService.isCompletedEvent(listedTwo)).thenReturn(true);
        when(eventService.isCompletedEvent(carouselUnique)).thenReturn(false);
        when(eventCategoryService.getAllCategories()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String view = publicViewHomeController.showUserHome(1, "rock", 7, "price_asc", null, model);

        assertEquals("user/userHome", view);
        assertEquals(1, model.get("currentPage"));
        assertEquals(1, model.get("totalPages"));
        assertEquals(2L, model.get("totalItems"));
        assertEquals("rock", model.get("currentSearch"));
        assertEquals(7, model.get("currentCategory"));
        assertEquals("price_asc", model.get("currentSort"));

        @SuppressWarnings("unchecked")
        var availabilityMap = (java.util.Map<Integer, Boolean>) model.get("availableTicketsByEventId");
        @SuppressWarnings("unchecked")
        var completedMap = (java.util.Map<Integer, Boolean>) model.get("completedEventsById");

        assertEquals(3, availabilityMap.size());
        assertEquals(true, availabilityMap.get(1));
        assertEquals(false, availabilityMap.get(2));
        assertEquals(true, availabilityMap.get(3));

        assertEquals(3, completedMap.size());
        assertEquals(false, completedMap.get(1));
        assertEquals(true, completedMap.get(2));
        assertEquals(false, completedMap.get(3));

        verify(eventService, times(1)).hasAvailableTicketsForEvent(1);
        verify(eventService, times(1)).hasAvailableTicketsForEvent(2);
        verify(eventService, times(1)).hasAvailableTicketsForEvent(3);
    }
}
