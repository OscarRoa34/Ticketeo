package co.edu.uptc.Ticketeo.interest.application;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.catalog.domain.Event;
import co.edu.uptc.Ticketeo.interest.domain.EventInterestDto;
import co.edu.uptc.Ticketeo.interest.domain.InterestReport;
import co.edu.uptc.Ticketeo.interest.infrastructure.repository.InterestReportRepository;
import co.edu.uptc.Ticketeo.user.domain.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InterestReportService {

    private final InterestReportRepository interestReportRepository;


    @Transactional
    public boolean toggleInterest(Event event, User user) {
        if (interestReportRepository.existsByEventIdAndUserId(event.getId(), user.getId())) {
            interestReportRepository.deleteByEventIdAndUserId(event.getId(), user.getId());
            return false;
        } else {
            InterestReport report = new InterestReport();
            report.setEvent(event);
            report.setUser(user);
            report.setRegistrationDate(LocalDateTime.now());
            interestReportRepository.save(report);
            return true;
        }
    }

    public boolean isUserInterested(Integer eventId, Long userId) {
        if (userId == null) return false;
        return interestReportRepository.existsByEventIdAndUserId(eventId, userId);
    }

    public List<EventInterestDto> getEventInterestRanking() {
        return interestReportRepository.findEventInterestRanking();
    }
}