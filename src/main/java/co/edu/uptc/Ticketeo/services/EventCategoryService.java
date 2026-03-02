package co.edu.uptc.Ticketeo.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.models.EventCategory;
import co.edu.uptc.Ticketeo.repository.EventCategoryModelRepository;
import co.edu.uptc.Ticketeo.repository.EventRepository;
import co.edu.uptc.Ticketeo.repository.InterestReportRepository;

@Service
public class EventCategoryService {

    private static final String[] COLOR_PALETTE = {
        "#E74C3C", "#3498DB", "#2ECC71", "#F39C12", "#9B59B6",
        "#1ABC9C", "#E67E22", "#2980B9", "#27AE60", "#8E44AD",
        "#16A085", "#D35400", "#C0392B", "#2C3E50", "#F1C40F",
        "#7F8C8D", "#6C5CE7", "#00B894", "#E17055", "#0984E3"
    };

    private final EventCategoryModelRepository eventCategoryRepository;
    private final EventRepository eventRepository;
    private final InterestReportRepository interestReportRepository;

    public EventCategoryService(EventCategoryModelRepository eventCategoryRepository,
                                EventRepository eventRepository,
                                InterestReportRepository interestReportRepository) {
        this.eventCategoryRepository = eventCategoryRepository;
        this.eventRepository = eventRepository;
        this.interestReportRepository = interestReportRepository;
    }

    public EventCategory saveCategory(EventCategory category) {
        if (category.getId() == null) {
            List<EventCategory> existing = eventCategoryRepository.findAll();
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

    public Page<EventCategory> getCategoriesPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return eventCategoryRepository.findAll(pageable);
    }

    public EventCategory getEventCategoryById(Integer id) {
        return eventCategoryRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteCategory(Integer id) {
        interestReportRepository.deleteByEventCategoryId(id);
        eventRepository.deleteByCategory_Id(id);
        eventCategoryRepository.deleteById(id);
    }
}
