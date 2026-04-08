package co.edu.uptc.Ticketeo.events.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.events.models.TicketType;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Integer> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
