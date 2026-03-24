package co.edu.uptc.Ticketeo.interest.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uptc.Ticketeo.events.models.Event;
import co.edu.uptc.Ticketeo.interest.models.EventInterestDto;
import co.edu.uptc.Ticketeo.interest.models.InterestReport;
import co.edu.uptc.Ticketeo.interest.repositories.InterestReportRepository;
import co.edu.uptc.Ticketeo.user.models.User;
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