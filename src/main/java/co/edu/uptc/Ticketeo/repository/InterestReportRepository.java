package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.dtos.EventInterestDto;
import co.edu.uptc.Ticketeo.models.InterestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterestReportRepository extends JpaRepository<InterestReport, Integer> {

    @Query("SELECT r.event AS event, COUNT(r.id) AS totalInterests FROM InterestReport r GROUP BY r.event ORDER BY totalInterests DESC")
    List<EventInterestDto> findEventInterestRanking();
}