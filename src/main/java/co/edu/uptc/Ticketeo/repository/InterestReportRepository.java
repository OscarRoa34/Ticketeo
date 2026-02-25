package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.models.InterestReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestReportRepository extends JpaRepository<InterestReport, Integer> {
}