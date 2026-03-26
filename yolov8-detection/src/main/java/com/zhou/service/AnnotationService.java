package com.zhou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhou.model.BoundingBox;
import com.zhou.model.ImageAnnotation;
import com.zhou.model.ImageAnnotationEntity;
import com.zhou.repository.ImageAnnotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class AnnotationService {
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final ImageAnnotationRepository imageAnnotationRepository;

    public AnnotationService(
            StorageService storageService,
            ObjectMapper objectMapper,
            ImageAnnotationRepository imageAnnotationRepository
    ) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.imageAnnotationRepository = imageAnnotationRepository;
    }

    public ImageAnnotation uploadImage(MultipartFile file, String batchId, String relativePath) throws IOException {
        String imageId = UUID.randomUUID().toString();
        String normalizedBatchId = sanitizeBatchId(batchId);
        String normalizedRelativePath = sanitizeRelativePath(relativePath, file.getOriginalFilename());

        Path batchRoot = storageService.imagesDir().resolve(normalizedBatchId);
        Path imagePath = batchRoot.resolve(normalizedRelativePath).normalize();
        if (!imagePath.startsWith(batchRoot)) {
            throw new IllegalStateException("Invalid relative path: " + relativePath);
        }
        Files.createDirectories(imagePath.getParent());
        Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);

        ImageAnnotation annotation = new ImageAnnotation();
        annotation.setImageId(imageId);
        annotation.setBatchId(normalizedBatchId);
        annotation.setRelativePath(normalizedRelativePath.replace('\\', '/'));
        annotation.setOriginalFileName(file.getOriginalFilename());
        annotation.setStoredPath(imagePath.toString());
        annotation.setAnnotated(false);
        annotation.setUploadedAt(Instant.now());
        annotation.setBoxes(List.of());
        saveAnnotation(annotation);
        return annotation;
    }

    public List<ImageAnnotation> listImages(String batchId) {
        List<ImageAnnotationEntity> entities = (batchId == null || batchId.isBlank())
                ? imageAnnotationRepository.findAll()
                : imageAnnotationRepository.findByBatchId(batchId);
        return entities.stream()
                .map(this::toModel)
                .sorted(Comparator.comparing(ImageAnnotation::getUploadedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public ImageAnnotation getById(String imageId) {
        ImageAnnotationEntity entity = imageAnnotationRepository.findById(imageId);
        if (entity == null) {
            throw new NoSuchElementException("Image not found: " + imageId);
        }
        return toModel(entity);
    }

    public ImageAnnotation updateBoxes(String imageId, List<BoundingBox> boxes) throws IOException {
        ImageAnnotation annotation = getById(imageId);
        annotation.setBoxes(boxes == null ? List.of() : boxes);
        annotation.setAnnotated(boxes != null && !boxes.isEmpty());
        saveAnnotation(annotation);
        refreshBatchLabels(annotation.getBatchId());
        return getById(imageId);
    }

    public void deleteImage(String imageId) throws IOException {
        ImageAnnotation annotation = getById(imageId);
        Files.deleteIfExists(resolveStoredImagePath(annotation));
        if (annotation.getLabelPath() != null && !annotation.getLabelPath().isBlank()) {
            Files.deleteIfExists(resolveSafePath(annotation.getLabelPath(), storageService.imagesDir()));
        }
        imageAnnotationRepository.deleteById(imageId);
    }

    public void clearAllImages() throws IOException {
        imageAnnotationRepository.deleteAll();
        deleteChildren(storageService.annotationsDir());
        deleteChildren(storageService.imagesDir());
    }

    private void refreshBatchLabels(String batchId) throws IOException {
        if (batchId == null || batchId.isBlank()) {
            return;
        }
        List<ImageAnnotation> batchImages = listImages(batchId);
        Map<String, Integer> classIndex = buildClassIndex(batchImages);
        for (ImageAnnotation item : batchImages) {
            Path labelPath = resolveLabelPath(item);
            Files.createDirectories(labelPath.getParent());
            writeYoloLabel(labelPath, item.getBoxes(), classIndex);
            item.setLabelPath(labelPath.toString());
            item.setAnnotated(item.getBoxes() != null && !item.getBoxes().isEmpty());
            saveAnnotation(item);
        }
    }

    private Map<String, Integer> buildClassIndex(List<ImageAnnotation> annotations) {
        List<String> labels = annotations.stream()
                .flatMap(a -> a.getBoxes().stream())
                .map(BoundingBox::label)
                .filter(label -> label != null && !label.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<String, Integer> classIndex = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            classIndex.put(labels.get(i), i);
        }
        return classIndex;
    }

    private Path resolveLabelPath(ImageAnnotation annotation) {
        Path imagePath = resolveStoredImagePath(annotation);
        Path batchRoot = storageService.imagesDir().resolve(sanitizeBatchId(annotation.getBatchId())).normalize();
        Path relative = batchRoot.relativize(imagePath);
        Path parent = relative.getParent();
        String imageName = relative.getFileName().toString();
        String labelName = imageName.contains(".")
                ? imageName.substring(0, imageName.lastIndexOf('.')) + ".txt"
                : imageName + ".txt";
        return parent == null
                ? batchRoot.resolve("labels").resolve(labelName)
                : batchRoot.resolve("labels").resolve(parent).resolve(labelName);
    }

    private void writeYoloLabel(Path labelPath, List<BoundingBox> boxes, Map<String, Integer> classIndex) throws IOException {
        if (boxes == null || boxes.isEmpty()) {
            Files.writeString(labelPath, "");
            return;
        }
        List<String> lines = new ArrayList<>();
        for (BoundingBox box : boxes) {
            Integer classId = classIndex.get(box.label());
            if (classId == null) {
                continue;
            }
            double x = resolveCenterX(box);
            double y = resolveCenterY(box);
            double width = resolveWidth(box);
            double height = resolveHeight(box);
            lines.add(classId + " " + fmt(x) + " " + fmt(y) + " " + fmt(width) + " " + fmt(height));
        }
        Files.write(labelPath, lines);
    }

    private double resolveCenterX(BoundingBox box) {
        if (!isPolygon(box)) {
            return box.x();
        }
        double minX = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::x).min().orElse(box.x());
        double maxX = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::x).max().orElse(box.x());
        return (minX + maxX) / 2.0;
    }

    private double resolveCenterY(BoundingBox box) {
        if (!isPolygon(box)) {
            return box.y();
        }
        double minY = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::y).min().orElse(box.y());
        double maxY = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::y).max().orElse(box.y());
        return (minY + maxY) / 2.0;
    }

    private double resolveWidth(BoundingBox box) {
        if (!isPolygon(box)) {
            return box.width();
        }
        double minX = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::x).min().orElse(box.x());
        double maxX = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::x).max().orElse(box.x());
        return Math.max(0.0, maxX - minX);
    }

    private double resolveHeight(BoundingBox box) {
        if (!isPolygon(box)) {
            return box.height();
        }
        double minY = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::y).min().orElse(box.y());
        double maxY = box.points().stream().mapToDouble(BoundingBox.PolygonPoint::y).max().orElse(box.y());
        return Math.max(0.0, maxY - minY);
    }

    private boolean isPolygon(BoundingBox box) {
        return box != null
                && "polygon".equalsIgnoreCase(String.valueOf(box.shapeType()))
                && box.points() != null
                && box.points().size() >= 3;
    }

    private String fmt(double value) {
        double bounded = Math.max(0.0, Math.min(1.0, value));
        return String.format(Locale.US, "%.6f", bounded);
    }

    private void saveAnnotation(ImageAnnotation annotation) {
        ImageAnnotationEntity entity = new ImageAnnotationEntity();
        entity.setImageId(annotation.getImageId());
        entity.setBatchId(annotation.getBatchId());
        entity.setRelativePath(annotation.getRelativePath());
        entity.setOriginalFileName(annotation.getOriginalFileName());
        entity.setStoredPath(annotation.getStoredPath());
        entity.setLabelPath(annotation.getLabelPath());
        entity.setAnnotated(annotation.isAnnotated() ? 1 : 0);
        entity.setUploadedAt(annotation.getUploadedAt() == null ? Instant.now() : annotation.getUploadedAt());
        entity.setBoxesJson(serializeBoxes(annotation.getBoxes()));
        imageAnnotationRepository.save(entity);
    }

    private ImageAnnotation toModel(ImageAnnotationEntity entity) {
        ImageAnnotation annotation = new ImageAnnotation();
        annotation.setImageId(entity.getImageId());
        annotation.setBatchId(entity.getBatchId());
        annotation.setRelativePath(entity.getRelativePath());
        annotation.setOriginalFileName(entity.getOriginalFileName());
        annotation.setStoredPath(entity.getStoredPath());
        annotation.setLabelPath(entity.getLabelPath());
        annotation.setUploadedAt(entity.getUploadedAt());
        List<BoundingBox> boxes = deserializeBoxes(entity.getBoxesJson());
        annotation.setBoxes(boxes);
        annotation.setAnnotated(!boxes.isEmpty());
        return annotation;
    }

    private String serializeBoxes(List<BoundingBox> boxes) {
        try {
            return objectMapper.writeValueAsString(boxes == null ? List.of() : boxes);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize boxes", ex);
        }
    }

    private List<BoundingBox> deserializeBoxes(String boxesJson) {
        if (boxesJson == null || boxesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(boxesJson, new TypeReference<>() {
            });
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    private Path resolveStoredImagePath(ImageAnnotation annotation) {
        return resolveSafePath(annotation.getStoredPath(), storageService.imagesDir());
    }

    private Path resolveSafePath(String value, Path root) {
        Path path = Path.of(value).toAbsolutePath().normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!path.startsWith(normalizedRoot)) {
            throw new IllegalStateException("Invalid stored path: " + value);
        }
        return path;
    }

    private void deleteChildren(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var stream = Files.list(directory)) {
            for (Path child : stream.toList()) {
                if (Files.isDirectory(child)) {
                    try (var walk = Files.walk(child)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                            }
                        });
                    }
                } else {
                    Files.deleteIfExists(child);
                }
            }
        }
    }

    private String sanitizeBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return "batch-" + Instant.now().toEpochMilli();
        }
        return batchId.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeRelativePath(String relativePath, String fallbackName) {
        String defaultName = fallbackName == null || fallbackName.isBlank() ? UUID.randomUUID() + ".jpg" : fallbackName;
        String normalized = (relativePath == null || relativePath.isBlank() ? defaultName : relativePath)
                .replace('\\', '/');
        normalized = normalized.replaceAll("^/+", "");
        if (normalized.contains("..")) {
            throw new IllegalStateException("Relative path contains invalid segment");
        }
        return normalized;
    }
}
