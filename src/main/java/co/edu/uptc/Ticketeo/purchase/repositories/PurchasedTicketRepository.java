package co.edu.uptc.Ticketeo.purchase.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;
import co.edu.uptc.Ticketeo.reports.models.TicketTypeSalesAggregationDto;

@Repository
public interface PurchasedTicketRepository extends JpaRepository<PurchasedTicket, Long> {

    boolean existsByPurchase_EventId(Integer eventId);

    @Query("""
            SELECT t
            FROM PurchasedTicket t
            JOIN FETCH t.purchase p
            WHERE t.id = :ticketId
              AND p.user.username = :username
            """)
    Optional<PurchasedTicket> findByIdAndUsername(@Param("ticketId") Long ticketId, @Param("username") String username);

    @Query("""
            SELECT t.ticketTypeName, COUNT(t)
            FROM PurchasedTicket t
            WHERE t.purchase.eventId = :eventId
            GROUP BY t.ticketTypeName
            """)
    List<Object[]> countSoldTicketsByTypeNameForEvent(@Param("eventId") Integer eventId);

    @Query("""
            SELECT t.ticketTypeName AS ticketTypeName,
                   COUNT(t) AS soldTickets,
                   COALESCE(SUM(t.unitPrice), 0) AS totalRevenue
            FROM PurchasedTicket t
            WHERE t.purchase.eventId = :eventId
            GROUP BY t.ticketTypeName
            """)
    List<TicketTypeSalesAggregationDto> summarizeTicketSalesByTypeForEvent(@Param("eventId") Integer eventId);
}
