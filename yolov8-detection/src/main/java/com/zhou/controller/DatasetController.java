package com.zhou.controller;

import com.zhou.dto.DatasetBuildRequest;
import com.zhou.service.DatasetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {
    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping("/build")
    public Map<String, Object> build(@Valid @RequestBody DatasetBuildRequest request) throws IOException {
        return datasetService.buildDataset(request);
    }

    @GetMapping("/directory-roots")
    public java.util.List<java.util.Map<String, String>> directoryRoots() {
        return datasetService.listDirectoryRoots();
    }

    @GetMapping("/directories")
    public Map<String, Object> directories(@RequestParam(value = "path", required = false) String path) throws IOException {
        return datasetService.listDirectories(path);
    }
}
