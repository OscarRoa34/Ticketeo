package co.edu.uptc.Ticketeo.configuration;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("toastErrorMessage")
    public String toastErrorMessage(HttpServletRequest request) {
        if (request.getParameter("uploadError") != null) {
            return "El archivo es demasiado grande. Tamaño máximo permitido: 50MB.";
        }
        if (request.getParameter("deactivateError") != null) {
            return "No se pudo desactivar el evento.";
        }
        return null;
    }
}
