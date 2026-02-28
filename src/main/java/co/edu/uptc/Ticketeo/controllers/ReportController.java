package co.edu.uptc.Ticketeo.controllers;

import co.edu.uptc.Ticketeo.services.InterestReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private final InterestReportService interestReportService;

    public ReportController(InterestReportService interestReportService) {
        this.interestReportService = interestReportService;
    }

    @GetMapping
    public String showReports(Model model) {
        model.addAttribute("interestRanking", interestReportService.getEventInterestRanking());
        return "reports";
    }
}
