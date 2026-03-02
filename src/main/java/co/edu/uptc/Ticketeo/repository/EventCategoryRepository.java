package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.models.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Integer> {
}
