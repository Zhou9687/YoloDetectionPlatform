package com.zhou.service;

import com.zhou.config.YoloProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class StorageService {
    private final Path workspace;
    private final Path imagesDir;
    private final Path annotationsDir;
    private final Path datasetsDir;
    private final Path predictResultsDir;

    public StorageService(YoloProperties properties) throws IOException {
        this.workspace = Path.of(properties.getWorkspace()).toAbsolutePath().normalize();
        this.imagesDir = workspace.resolve("images");
        this.annotationsDir = workspace.resolve("annotations");
        this.datasetsDir = workspace.resolve("datasets");
        this.predictResultsDir = workspace.resolve("predict-results");
        Files.createDirectories(workspace);
        Files.createDirectories(imagesDir);
        Files.createDirectories(annotationsDir);
        Files.createDirectories(datasetsDir);
        Files.createDirectories(predictResultsDir);
    }

    public Path workspace() {
        return workspace;
    }

    public Path imagesDir() {
        return imagesDir;
    }

    public Path annotationsDir() {
        return annotationsDir;
    }

    public Path datasetsDir() {
        return datasetsDir;
    }

    public Path predictResultsDir() {
        return predictResultsDir;
    }
}
