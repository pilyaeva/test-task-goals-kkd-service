package com.example.testtaskgoalskkd.api;

import com.example.testtaskgoalskkd.api.dto.IssuesResponse;
import com.example.testtaskgoalskkd.api.dto.KkdSummaryResponse;
import com.example.testtaskgoalskkd.application.IssuesService;
import com.example.testtaskgoalskkd.application.KkdSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kkd")
public class KkdController {

    private final KkdSummaryService kkdSummaryService;
    private final IssuesService issuesService;

    public KkdController(KkdSummaryService kkdSummaryService, IssuesService issuesService) {
        this.kkdSummaryService = kkdSummaryService;
        this.issuesService = issuesService;
    }

    @GetMapping("/issues")
    public IssuesResponse getIssues(
            @RequestParam("run_id") String runId,
            @RequestParam(name = "unit_id", required = false) String unitId,
            @RequestParam(name = "rule_code", required = false) String ruleCode,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Long offset) {
        return IssuesResponse.from(issuesService.getIssues(runId, unitId, ruleCode, severity, limit, offset));
    }

    @GetMapping("/summary")
    public KkdSummaryResponse getSummary(
            @RequestParam("run_id") String runId, @RequestParam("unit_id") String unitId) {
        return KkdSummaryResponse.from(kkdSummaryService.getSummary(runId, unitId));
    }
}
