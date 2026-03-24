package co.edu.uptc.Ticketeo.interest.controllers;

import co.edu.uptc.Ticketeo.interest.services.InterestReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final InterestReportService interestReportService;

    @GetMapping({"", "/"})
    public String showReportsMenu() {
        return "interest/adminReportsMenu";
    }

    @GetMapping("/interest")
    public String showInterestReport(Model model) {
        model.addAttribute("interestRanking", interestReportService.getEventInterestRanking());
        return "interest/adminInterestReport";
    }
}
