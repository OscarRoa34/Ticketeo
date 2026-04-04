package co.edu.uptc.Ticketeo.purchase.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.purchase.models.Purchase;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

        @Query("SELECT COALESCE(SUM(p.totalPaid), 0.0) FROM Purchase p")
        Double getTotalCompanyRevenue();

    @Query("""
            SELECT DISTINCT p
            FROM Purchase p
            LEFT JOIN FETCH p.tickets t
            WHERE p.user.username = :username
            ORDER BY p.purchaseDate DESC, p.id DESC
            """)
    List<Purchase> findAllByUsernameWithTickets(@Param("username") String username);

    @Query("""
            SELECT DISTINCT p
            FROM Purchase p
            LEFT JOIN FETCH p.tickets t
            WHERE p.id = :purchaseId
              AND p.user.username = :username
            """)
    Optional<Purchase> findByIdAndUsernameWithTickets(@Param("purchaseId") Long purchaseId, @Param("username") String username);
}

