package com.zhou.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ImageAnnotation {
    private String imageId;
    private String batchId;
    private String relativePath;
    private String originalFileName;
    private String storedPath;
    private String labelPath;
    private boolean annotated;
    private Instant uploadedAt;
    private List<BoundingBox> boxes = new ArrayList<>();

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;
    }

    public String getLabelPath() {
        return labelPath;
    }

    public void setLabelPath(String labelPath) {
        this.labelPath = labelPath;
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public List<BoundingBox> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<BoundingBox> boxes) {
        this.boxes = boxes;
    }
}
