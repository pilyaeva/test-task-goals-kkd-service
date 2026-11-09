package com.example.testtask.api;

import com.example.testtask.api.dto.KkdSummaryResponse;
import com.example.testtask.application.KkdSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kkd")
public class KkdController {

    private final KkdSummaryService kkdSummaryService;

    public KkdController(KkdSummaryService kkdSummaryService) {
        this.kkdSummaryService = kkdSummaryService;
    }

    @GetMapping("/summary")
    public KkdSummaryResponse getSummary(
            @RequestParam("run_id") String runId, @RequestParam("unit_id") String unitId) {
        return KkdSummaryResponse.from(kkdSummaryService.getSummary(runId, unitId));
    }
}
