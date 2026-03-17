package co.edu.uptc.Ticketeo.catalog.infrastructure.repository;

import co.edu.uptc.Ticketeo.catalog.domain.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Integer> {
}
