package co.edu.uptc.Ticketeo.services;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.models.EventCategory;
import co.edu.uptc.Ticketeo.repository.EventCategoryModelRepository;

@Service
public class EventCategoryService {

    private static final String[] COLOR_PALETTE = {
        "#E74C3C", "#3498DB", "#2ECC71", "#F39C12", "#9B59B6",
        "#1ABC9C", "#E67E22", "#2980B9", "#27AE60", "#8E44AD",
        "#16A085", "#D35400", "#C0392B", "#2C3E50", "#F1C40F",
        "#7F8C8D", "#6C5CE7", "#00B894", "#E17055", "#0984E3"
    };

    private final EventCategoryModelRepository eventCategoryRepository;

    public EventCategoryService(EventCategoryModelRepository eventCategoryRepository) {
        this.eventCategoryRepository = eventCategoryRepository;
    }

    public EventCategory saveCategory(EventCategory category) {
        // Si es nueva categoría (sin ID), asignar color único
        if (category.getId() == null) {
            List<EventCategory> existing = eventCategoryRepository.findAll();
            // Buscar un color de la paleta que no esté en uso
            String assignedColor = COLOR_PALETTE[existing.size() % COLOR_PALETTE.length];
            for (String color : COLOR_PALETTE) {
                boolean colorInUse = existing.stream().anyMatch(c -> color.equals(c.getColor()));
                if (!colorInUse) {
                    assignedColor = color;
                    break;
                }
            }
            category.setColor(assignedColor);
        }
        return eventCategoryRepository.save(category);
    }

    public List<EventCategory> getAllCategories() {
        return eventCategoryRepository.findAll();
    }

    public EventCategory getEventCategoryById(Integer id) {
        return eventCategoryRepository.findById(id).orElse(null);
    }

    public void deleteCategory(Integer id) {
        eventCategoryRepository.deleteById(id);
    }
}
