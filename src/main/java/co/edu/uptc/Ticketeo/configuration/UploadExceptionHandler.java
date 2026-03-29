package co.edu.uptc.Ticketeo.configuration;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(annotations = Controller.class)
public class UploadExceptionHandler {

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class, IllegalStateException.class})
    public String handleUploadExceptions(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin/event")) {
            return "redirect:" + appendUploadErrorParam(referer);
        }
        return "redirect:/admin/event/new?uploadError=size";
    }

    private String appendUploadErrorParam(String url) {
        if (url.contains("uploadError=")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "uploadError=size";
    }
}
