package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.services.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import co.edu.uptc.Ticketeo.services.EventCategoryService;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final EventService eventService;
    private final EventCategoryService eventCategoryService;

    public AdminPageController(EventService eventService, EventCategoryService eventCategoryService) {
        this.eventService = eventService;
        this.eventCategoryService = eventCategoryService;
    }

    @GetMapping
    public String showAdminPage(Model model) {
        model.addAttribute("eventos", eventService.getAllEvents());
        return "admin";
    }

    @GetMapping("/evento/nuevo")
    public String showEventForm(Model model) {
        model.addAttribute("evento", new Event());
        model.addAttribute("categorias", eventCategoryService.getAllCategories());
        return "eventForm";
    }

    @PostMapping("/evento/guardar")
    public String saveEvent(@ModelAttribute Event evento,
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

                evento.setUrlImagen("/uploads/" + imagen.getOriginalFilename());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (evento.getId() != null) {
                Event existingEvent = eventService.getEventById(evento.getId());
                if (existingEvent != null) {
                    evento.setUrlImagen(existingEvent.getUrlImagen());
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