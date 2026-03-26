package com.zhou.controller;

import com.zhou.dto.TrainingStartRequest;
import com.zhou.model.TrainingJob;
import com.zhou.service.TrainingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training")
public class TrainingController {
    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping("/start")
    public TrainingJob start(@Valid @RequestBody TrainingStartRequest request) {
        return trainingService.start(
                request.datasetPath(),
                request.epochs(),
                request.batch(),
                request.modelPath(),
                request.conf()
        );
    }

    @GetMapping("/{jobId}")
    public TrainingJob get(@PathVariable String jobId) {
        return trainingService.get(jobId);
    }
}
