package co.edu.uptc.Ticketeo.events.controllers.admin;

import org.springframework.ui.Model;

final class EventFormNavigation {

    private static final String EVENT_NEW_PATH = "/admin/event/new";
    private static final String EVENT_EDIT_PATH_PREFIX = "/admin/event/edit/";

    private EventFormNavigation() {
    }

    static Integer sanitizeEventId(Integer eventId) {
        if (eventId == null || eventId <= 0) {
            return null;
        }
        return eventId;
    }

    static String buildEventFormPath(Integer eventId, boolean draft) {
        Integer safeEventId = sanitizeEventId(eventId);
        if (safeEventId != null) {
            return EVENT_EDIT_PATH_PREFIX + safeEventId;
        }
        return draft ? EVENT_NEW_PATH + "?draft=true" : EVENT_NEW_PATH;
    }

    static String buildEventFormRedirect(Integer eventId,
                                         boolean draft,
                                         String selectedParamName,
                                         Integer selectedValue) {
        String eventFormPath = buildEventFormPath(eventId, draft);
        if (selectedValue == null) {
            return "redirect:" + eventFormPath;
        }

        String separator = eventFormPath.contains("?") ? "&" : "?";
        return "redirect:" + eventFormPath + separator + selectedParamName + "=" + selectedValue;
    }

    static void populateFormContext(Model model,
                                    boolean fromEventForm,
                                    Integer eventId,
                                    boolean draft,
                                    String fallbackCancelPath) {
        Integer safeEventId = sanitizeEventId(eventId);
        model.addAttribute("fromEventForm", fromEventForm);
        model.addAttribute("eventId", safeEventId);
        model.addAttribute("draft", draft);
        model.addAttribute("cancelPath", fromEventForm ? buildEventFormPath(safeEventId, draft) : fallbackCancelPath);
    }

    static String resolvePostSaveRedirect(boolean fromEventForm,
                                          Integer eventId,
                                          boolean draft,
                                          String selectedParamName,
                                          Integer selectedValue,
                                          String fallbackRedirectPath) {
        if (!fromEventForm) {
            return fallbackRedirectPath;
        }
        Integer safeEventId = sanitizeEventId(eventId);
        return buildEventFormRedirect(safeEventId, draft, selectedParamName, selectedValue);
    }
}
