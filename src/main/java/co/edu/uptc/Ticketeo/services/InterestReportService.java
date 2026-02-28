package co.edu.uptc.Ticketeo.services;


import co.edu.uptc.Ticketeo.models.Event;
import co.edu.uptc.Ticketeo.models.InterestReport;
import co.edu.uptc.Ticketeo.repository.InterestReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class InterestReportService {

    private final InterestReportRepository interestReportRepository;

    public InterestReportService(InterestReportRepository interestReportRepository) {
        this.interestReportRepository = interestReportRepository;
    }

    public void registrarInteres(Event evento) {
        InterestReport reporte = new InterestReport();
        reporte.setEvento(evento);
        reporte.setFechaRegistro(LocalDateTime.now());
        interestReportRepository.save(reporte);
    }
}
