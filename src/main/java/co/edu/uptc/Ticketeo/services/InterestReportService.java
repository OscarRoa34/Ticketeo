package co.edu.uptc.Ticketeo.services;

import co.edu.uptc.Ticketeo.dtos.EventInterestDto;
import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.models.InterestReport;
import co.edu.uptc.Ticketeo.repository.InterestReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterestReportService {

    private final InterestReportRepository interestReportRepository;

    public InterestReportService(InterestReportRepository interestReportRepository) {
        this.interestReportRepository = interestReportRepository;
    }

    public void registerInterest(Event event) {
        InterestReport report = new InterestReport();
        report.setEvent(event);
        report.setRegistrationDate(LocalDateTime.now());
        interestReportRepository.save(report);
    }

    public List<EventInterestDto> getEventInterestRanking() {
        return interestReportRepository.findEventInterestRanking();
    }
}