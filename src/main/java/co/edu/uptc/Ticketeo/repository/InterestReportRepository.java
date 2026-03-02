package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.dtos.EventInterestDto;
import co.edu.uptc.Ticketeo.models.InterestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestReportRepository extends JpaRepository<InterestReport, Integer> {

    @Query("SELECT r.event AS event, COUNT(r.id) AS totalInterests FROM InterestReport r GROUP BY r.event ORDER BY totalInterests DESC")
    List<EventInterestDto> findEventInterestRanking();

    @Modifying
    @Query("DELETE FROM InterestReport r WHERE r.event.id = :eventId")
    void deleteByEventId(Integer eventId);

    @Modifying
    @Query("DELETE FROM InterestReport r WHERE r.event.category.id = :categoryId")
    void deleteByEventCategoryId(Integer categoryId);
}