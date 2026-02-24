package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.models.EventCategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCategoryModelRepository extends JpaRepository<EventCategoryModel, Integer> {
}
