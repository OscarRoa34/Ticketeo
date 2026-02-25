package co.edu.uptc.Ticketeo.services;

import co.edu.uptc.Ticketeo.models.EventCategory;
import co.edu.uptc.Ticketeo.repository.EventCategoryModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void deleteCategory(Integer id) {
        eventCategoryRepository.deleteById(id);
    }
}
