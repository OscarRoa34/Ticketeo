package co.edu.uptc.Ticketeo.events.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.events.models.EventCategory;

@Repository
public interface EventCategoryRepository extends JpaRepository<EventCategory, Integer> {

	EventCategory findByNameIgnoreCase(String name);
}
