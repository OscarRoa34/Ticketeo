package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.EventModel;
import co.edu.uptc.Ticketeo.services.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final EventService eventService;

    public AdminPageController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public String showAdminPage(Model model) {
        model.addAttribute("eventos", eventService.getAllEvents());
        return "admin";
    }

    @GetMapping("/evento/nuevo")
    public String showEventForm(Model model) {
        model.addAttribute("evento", new EventModel());
        return "eventForm";
    }

    @PostMapping("/evento/guardar")
    public String saveEvent(@ModelAttribute EventModel evento,
                            @RequestParam("archivoImagen") MultipartFile imagen) {

        if (!imagen.isEmpty()) {
            Path directorioImagenes = Paths.get("src/main/resources/static/uploads");
            String rutaAbsoluta = directorioImagenes.toFile().getAbsolutePath();

            try {
                if (!Files.exists(directorioImagenes)) {
                    Files.createDirectories(directorioImagenes);
                }

                byte[] bytesImg = imagen.getBytes();
                Path rutaCompleta = Paths.get(rutaAbsoluta + "/" + imagen.getOriginalFilename());
                Files.write(rutaCompleta, bytesImg);

                evento.setImagenEvento("/uploads/" + imagen.getOriginalFilename());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (evento.getIdEvento() != null) {
                EventModel existingEvent = eventService.getEventById(evento.getIdEvento());
                if (existingEvent != null) {
                    evento.setImagenEvento(existingEvent.getImagenEvento());
                }
            }
        }

        eventService.saveEvent(evento);

        return "redirect:/admin";
    }

    @GetMapping("/evento/editar/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("evento", eventService.getEventById(id));
        return "eventForm";
    }

    @GetMapping("/evento/eliminar/{id}")
    public String deleteEvent(@PathVariable Integer id) {
        eventService.deleteEvent(id);
        return "redirect:/admin";
    }
}