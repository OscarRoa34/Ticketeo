package co.edu.uptc.Ticketeo.events.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.events.models.EventTicketType;

@Repository
public interface EventTicketTypeRepository extends JpaRepository<EventTicketType, Integer> {

    List<EventTicketType> findByEvent_Id(Integer eventId);

    @Query("""
            SELECT MIN(ett.ticketPrice)
            FROM EventTicketType ett
            WHERE ett.event.id = :eventId
              AND ett.ticketPrice IS NOT NULL
              AND ett.ticketPrice > 0
              AND ett.availableQuantity IS NOT NULL
              AND ett.availableQuantity > 0
            """)
    Double findMinimumAvailableTicketPriceByEventId(@Param("eventId") Integer eventId);

    @Query("SELECT DISTINCT ett.event.id FROM EventTicketType ett WHERE ett.ticketType.id = :ticketTypeId")
    List<Integer> findDistinctEventIdsByTicketTypeId(@Param("ticketTypeId") Integer ticketTypeId);

    void deleteByEvent_Id(Integer eventId);

    void deleteByTicketType_Id(Integer ticketTypeId);
}
