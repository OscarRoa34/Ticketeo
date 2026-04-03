package co.edu.uptc.Ticketeo.purchase.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.purchase.models.PurchasedTicket;

@Repository
public interface PurchasedTicketRepository extends JpaRepository<PurchasedTicket, Long> {

    @Query("""
            SELECT t
            FROM PurchasedTicket t
            JOIN FETCH t.purchase p
            WHERE t.id = :ticketId
              AND p.user.username = :username
            """)
    Optional<PurchasedTicket> findByIdAndUsername(@Param("ticketId") Long ticketId, @Param("username") String username);
}

