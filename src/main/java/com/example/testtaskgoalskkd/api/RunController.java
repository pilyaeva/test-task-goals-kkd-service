package com.example.testtaskgoalskkd.api;

import com.example.testtaskgoalskkd.api.dto.RunResponse;
import com.example.testtaskgoalskkd.application.RunService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public ResponseEntity<RunResponse> startRun() {
        return ResponseEntity.status(HttpStatus.CREATED).body(RunResponse.from(runService.startRun()));
    }
}
