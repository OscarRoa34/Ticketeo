package co.edu.uptc.Ticketeo.events.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.events.models.EventTicketType;

@Repository
public interface EventTicketTypeRepository extends JpaRepository<EventTicketType, Integer> {

    List<EventTicketType> findByEvent_Id(Integer eventId);

    void deleteByEvent_Id(Integer eventId);

    void deleteByTicketType_Id(Integer ticketTypeId);
}
