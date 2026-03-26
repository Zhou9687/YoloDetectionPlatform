package com.zhou.model;

import java.time.Instant;

public class ImageAnnotationEntity {
    private String imageId;
    private String batchId;
    private String relativePath;
    private String originalFileName;
    private String storedPath;
    private String labelPath;
    private Integer annotated;
    private Instant uploadedAt;
    private String boxesJson;

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

    public Integer getAnnotated() {
        return annotated;
    }

    public void setAnnotated(Integer annotated) {
        this.annotated = annotated;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getBoxesJson() {
        return boxesJson;
    }

    public void setBoxesJson(String boxesJson) {
        this.boxesJson = boxesJson;
    }
}

