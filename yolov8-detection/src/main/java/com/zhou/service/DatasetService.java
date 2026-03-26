package com.zhou.service;

import com.zhou.dto.DatasetBuildRequest;
import com.zhou.model.BoundingBox;
import com.zhou.model.DatasetBuildRecordEntity;
import com.zhou.model.ImageAnnotation;
import com.zhou.repository.DatasetBuildRecordRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class DatasetService {
    private final AnnotationService annotationService;
    private final StorageService storageService;
    private final DatasetBuildRecordRepository datasetBuildRecordRepository;

    public DatasetService(
            AnnotationService annotationService,
            StorageService storageService,
            DatasetBuildRecordRepository datasetBuildRecordRepository
    ) {
        this.annotationService = annotationService;
        this.storageService = storageService;
        this.datasetBuildRecordRepository = datasetBuildRecordRepository;
    }

    public Map<String, Object> buildDataset(DatasetBuildRequest request) throws IOException {
        Path outputBase = resolveOutputBase(request.outputPath(), request.outputPreset());
        Path root = outputBase.resolve(request.datasetName()).toAbsolutePath().normalize();

        Path trainImgs = root.resolve("train/images");
        Path trainLabels = root.resolve("train/labels");
        Path valImgs = root.resolve("val/images");
        Path valLabels = root.resolve("val/labels");
        Path testImgs = root.resolve("test/images");
        Path testLabels = root.resolve("test/labels");

        Files.createDirectories(trainImgs);
        Files.createDirectories(trainLabels);
        Files.createDirectories(valImgs);
        Files.createDirectories(valLabels);
        Files.createDirectories(testImgs);
        Files.createDirectories(testLabels);

        List<ImageAnnotation> all = annotationService.listImages(null).stream()
                .filter(item -> item.getBoxes() != null && !item.getBoxes().isEmpty())
                .filter(item -> item.getLabelPath() != null && !item.getLabelPath().isBlank())
                .sorted(Comparator.comparing(ImageAnnotation::getImageId))
                .toList();

        if (all.isEmpty()) {
            throw new IllegalStateException("No annotated images found, please annotate and save first.");
        }

        List<ImageAnnotation> shuffled = new ArrayList<>(all);
        shuffled.sort(Comparator.comparing(ImageAnnotation::getImageId));
        java.util.Collections.shuffle(shuffled, new Random(42));

        int valCount = (int) Math.round(shuffled.size() * request.valRatio());
        int testCount = (int) Math.round(shuffled.size() * request.testRatio());
        if (valCount + testCount > shuffled.size()) {
            testCount = Math.max(0, shuffled.size() - valCount);
        }

        List<ImageAnnotation> valSet = shuffled.subList(0, Math.min(valCount, shuffled.size()));
        List<ImageAnnotation> testSet = shuffled.subList(Math.min(valCount, shuffled.size()), Math.min(valCount + testCount, shuffled.size()));

        // train contains full source set as requested.
        for (ImageAnnotation item : all) {
            copyPair(item, trainImgs, trainLabels);
        }
        for (ImageAnnotation item : valSet) {
            copyPair(item, valImgs, valLabels);
        }
        for (ImageAnnotation item : testSet) {
            copyPair(item, testImgs, testLabels);
        }

        writeYaml(root.resolve("data.yaml"), all);

        Instant now = Instant.now();
        Map<String, Object> response = new HashMap<>();
        response.put("datasetPath", root.toString());
        response.put("outputPreset", normalizePreset(request.outputPreset()));
        response.put("trainCount", all.size());
        response.put("valCount", valSet.size());
        response.put("testCount", testSet.size());
        response.put("createdAt", now);

        DatasetBuildRecordEntity record = new DatasetBuildRecordEntity();
        record.setRecordId(UUID.randomUUID().toString());
        record.setDatasetName(request.datasetName());
        record.setDatasetPath(root.toString());
        record.setOutputPreset(String.valueOf(response.get("outputPreset")));
        record.setTrainCount(all.size());
        record.setValCount(valSet.size());
        record.setTestCount(testSet.size());
        record.setValRatio(request.valRatio());
        record.setTestRatio(request.testRatio());
        record.setCreatedAt(now);
        datasetBuildRecordRepository.save(record);

        return response;
    }

    private Path resolveOutputBase(String outputPath, String outputPreset) {
        if (outputPath != null && !outputPath.isBlank()) {
            Path path = Path.of(outputPath).toAbsolutePath().normalize();
            if (path.startsWith(storageService.workspace()) || path.isAbsolute()) {
                return path;
            }
            throw new IllegalStateException("Invalid output path");
        }

        String preset = normalizePreset(outputPreset);
        return switch (preset) {
            case "WORKSPACE_ROOT" -> storageService.workspace();
            case "WORKSPACE_DATASETS" -> storageService.datasetsDir();
            default -> throw new IllegalStateException("Invalid output preset: " + preset);
        };
    }

    private String normalizePreset(String outputPreset) {
        if (outputPreset == null || outputPreset.isBlank()) {
            return "WORKSPACE_DATASETS";
        }
        return outputPreset.trim().toUpperCase(Locale.ROOT);
    }

    private void copyPair(ImageAnnotation item, Path imageTargetDir, Path labelTargetDir) throws IOException {
        Path imageSource = Path.of(item.getStoredPath()).toAbsolutePath().normalize();
        Path labelSource = Path.of(item.getLabelPath()).toAbsolutePath().normalize();
        if (!Files.exists(imageSource) || !Files.exists(labelSource)) {
            return;
        }

        String imageName = imageSource.getFileName().toString();
        String stem = imageName.contains(".") ? imageName.substring(0, imageName.lastIndexOf('.')) : imageName;
        String imageExt = imageName.contains(".") ? imageName.substring(imageName.lastIndexOf('.')) : ".jpg";

        Path imageTarget = imageTargetDir.resolve(stem + imageExt);
        Path labelTarget = labelTargetDir.resolve(stem + ".txt");

        Files.copy(imageSource, imageTarget, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(labelSource, labelTarget, StandardCopyOption.REPLACE_EXISTING);
    }

    private void writeYaml(Path yamlPath, List<ImageAnnotation> annotations) throws IOException {
        List<String> sortedNames = annotations.stream()
                .flatMap(item -> item.getBoxes().stream())
                .map(BoundingBox::label)
                .filter(label -> label != null && !label.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        String yaml = "path: " + yamlPath.getParent().toAbsolutePath() + "\n"
                + "train: train/images\n"
                + "val: val/images\n"
                + "test: test/images\n"
                + "nc: " + sortedNames.size() + "\n"
                + "names: [" + String.join(", ", sortedNames) + "]\n";
        Files.writeString(yamlPath, yaml);
    }

    @SuppressWarnings("unused")
    private String clamp(double value) {
        double bounded = Math.max(0.0, Math.min(1.0, value));
        return String.format(Locale.US, "%.6f", bounded);
    }

    public List<Map<String, String>> listDirectoryRoots() {
        File[] roots = File.listRoots();
        if (roots == null) {
            return List.of();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (File root : roots) {
            String path = root.toPath().toAbsolutePath().normalize().toString();
            result.add(directoryNode(path, path));
        }
        return result;
    }

    public Map<String, Object> listDirectories(String rawPath) throws IOException {
        Path current = (rawPath == null || rawPath.isBlank())
                ? storageService.workspace()
                : Path.of(rawPath).toAbsolutePath().normalize();
        if (!Files.exists(current) || !Files.isDirectory(current)) {
            throw new IllegalStateException("Directory not found: " + current);
        }

        List<Map<String, String>> directories;
        try (var stream = Files.list(current)) {
            directories = stream
                    .filter(Files::isDirectory)
                    .map(path -> directoryNode(path.getFileName() == null ? path.toString() : path.getFileName().toString(), path.toString()))
                    .sorted(Comparator.comparing(item -> item.get("name"), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (UncheckedIOException ex) {
            throw ex.getCause();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("currentPath", current.toString());
        response.put("parentPath", current.getParent() == null ? null : current.getParent().toString());
        response.put("directories", directories);
        return response;
    }

    private Map<String, String> directoryNode(String name, String path) {
        Map<String, String> node = new HashMap<>();
        node.put("name", name);
        node.put("path", path);
        return node;
    }
}
