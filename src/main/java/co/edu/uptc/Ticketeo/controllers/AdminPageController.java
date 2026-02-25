package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.models.EventModel;
import co.edu.uptc.Ticketeo.services.EventCategoryService;
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

    @GetMapping("/nuevo-evento")
    public String showEventForm(Model model) {
        model.addAttribute("evento", new EventModel());
        model.addAttribute("categorias", eventCategoryService.getAllCategories());
        return "eventForm";
    }

    @PostMapping("/guardar-evento")
    public String saveEvent(@ModelAttribute EventModel evento,
                            @RequestParam("archivoImagen") MultipartFile imagen) {

        if (!imagen.isEmpty()) {
            Path directorioImagenes = Paths.get("src/main/resources/static/uploads");
            String rutaAbsoluta = directorioImagenes.toFile().getAbsolutePath();

            try {
                if(!Files.exists(directorioImagenes)){
                    Files.createDirectories(directorioImagenes);
                }

                byte[] bytesImg = imagen.getBytes();
                Path rutaCompleta = Paths.get(rutaAbsoluta + "/" + imagen.getOriginalFilename());
                Files.write(rutaCompleta, bytesImg);

                evento.setImagenEvento(imagen.getOriginalFilename());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        eventService.saveEvent(evento);

        return "redirect:/admin";
    }
}
