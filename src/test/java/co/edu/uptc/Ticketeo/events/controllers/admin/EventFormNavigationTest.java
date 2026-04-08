package co.edu.uptc.Ticketeo.events.controllers.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class EventFormNavigationTest {

    @Test
    void sanitizeEventId_invalidValues_returnsNull() {
        assertNull(EventFormNavigation.sanitizeEventId(null));
        assertNull(EventFormNavigation.sanitizeEventId(0));
        assertNull(EventFormNavigation.sanitizeEventId(-4));
    }

    @Test
    void buildEventFormPath_newEventDraftAndNonDraft_buildsExpectedPaths() {
        assertEquals("/admin/event/new", EventFormNavigation.buildEventFormPath(null, false));
        assertEquals("/admin/event/new?draft=true", EventFormNavigation.buildEventFormPath(null, true));
    }

    @Test
    void buildEventFormPath_withValidEventId_buildsEditPath() {
        assertEquals("/admin/event/edit/11", EventFormNavigation.buildEventFormPath(11, false));
    }

    @Test
    void buildEventFormRedirect_withoutSelectedValue_returnsBaseRedirect() {
        String redirect = EventFormNavigation.buildEventFormRedirect(5, false, "selectedTicketTypeId", null);

        assertEquals("redirect:/admin/event/edit/5", redirect);
    }

    @Test
    void buildEventFormRedirect_withSelectedValue_appendsWithQuerySeparator() {
        String redirectEdit = EventFormNavigation.buildEventFormRedirect(9, false, "selectedCategoryId", 22);
        String redirectNewDraft = EventFormNavigation.buildEventFormRedirect(null, true, "selectedTicketTypeId", 15);

        assertEquals("redirect:/admin/event/edit/9?selectedCategoryId=22", redirectEdit);
        assertEquals("redirect:/admin/event/new?draft=true&selectedTicketTypeId=15", redirectNewDraft);
    }

    @Test
    void populateFormContext_withEventForm_setsEventCancelPath() {
        ExtendedModelMap model = new ExtendedModelMap();

        EventFormNavigation.populateFormContext(model, true, 5, false, "/admin/category");

        assertEquals(true, model.get("fromEventForm"));
        assertEquals(5, model.get("eventId"));
        assertEquals(false, model.get("draft"));
        assertEquals("/admin/event/edit/5", model.get("cancelPath"));
    }

    @Test
    void populateFormContext_withoutEventForm_setsFallbackCancelPath() {
        ExtendedModelMap model = new ExtendedModelMap();

        EventFormNavigation.populateFormContext(model, false, null, true, "/admin/ticket-type");

        assertEquals(false, model.get("fromEventForm"));
        assertEquals(null, model.get("eventId"));
        assertEquals(true, model.get("draft"));
        assertEquals("/admin/ticket-type", model.get("cancelPath"));
    }

    @Test
    void resolvePostSaveRedirect_usesFallbackWhenNotFromEventForm() {
        String redirect = EventFormNavigation.resolvePostSaveRedirect(false, 2, false,
                "selectedTicketTypeId", 20, "redirect:/admin/ticket-type");

        assertEquals("redirect:/admin/ticket-type", redirect);
    }
}
