package co.edu.uptc.Ticketeo.services;

import co.edu.uptc.Ticketeo.models.EventCategoryModel;
import co.edu.uptc.Ticketeo.repository.EventCategoryModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventCategoryService {

    private final EventCategoryModelRepository eventCategoryRepository;

    public EventCategoryService(EventCategoryModelRepository eventCategoryRepository) {
        this.eventCategoryRepository = eventCategoryRepository;
    }

    public EventCategoryModel saveCategory(EventCategoryModel category) {
        return eventCategoryRepository.save(category);
    }

    public List<EventCategoryModel> getAllCategories() {
        return eventCategoryRepository.findAll();
    }

    public void deleteCategory(Integer id) {
        eventCategoryRepository.deleteById(id);
    }
}
