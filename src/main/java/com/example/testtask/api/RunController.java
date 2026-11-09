package com.example.testtask.api;

import com.example.testtask.api.dto.RunResponse;
import com.example.testtask.application.RunService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping
    public List<RunResponse> getRuns() {
        return runService.getRuns().stream().map(RunResponse::from).toList();
    }
}
