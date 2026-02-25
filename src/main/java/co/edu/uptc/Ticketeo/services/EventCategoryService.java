package co.edu.uptc.Ticketeo.services;

import java.util.List;

import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.models.EventCategory;
import co.edu.uptc.Ticketeo.repository.EventCategoryModelRepository;

@Service
public class EventCategoryService {

    private final EventCategoryModelRepository eventCategoryRepository;

    public EventCategoryService(EventCategoryModelRepository eventCategoryRepository) {
        this.eventCategoryRepository = eventCategoryRepository;
    }

    public EventCategory saveCategory(EventCategory category) {
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
