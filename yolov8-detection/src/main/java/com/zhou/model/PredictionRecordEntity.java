package com.zhou.model;

import java.time.Instant;

public class PredictionRecordEntity {
    private String resultId;
    private String fileName;
    private String relativePath;
    private String resultImagePath;
    private String modelPath;
    private Double conf;
    private Double iou;
    private String device;
    private Integer imgsz;
    private Integer augment;
    private Integer maxDet;
    private String boxesJson;
    private Instant createdAt;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getResultImagePath() {
        return resultImagePath;
    }

    public void setResultImagePath(String resultImagePath) {
        this.resultImagePath = resultImagePath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public Double getConf() {
        return conf;
    }

    public void setConf(Double conf) {
        this.conf = conf;
    }

    public Double getIou() {
        return iou;
    }

    public void setIou(Double iou) {
        this.iou = iou;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Integer getImgsz() {
        return imgsz;
    }

    public void setImgsz(Integer imgsz) {
        this.imgsz = imgsz;
    }

    public Integer getAugment() {
        return augment;
    }

    public void setAugment(Integer augment) {
        this.augment = augment;
    }

    public Integer getMaxDet() {
        return maxDet;
    }

    public void setMaxDet(Integer maxDet) {
        this.maxDet = maxDet;
    }

    public String getBoxesJson() {
        return boxesJson;
    }

    public void setBoxesJson(String boxesJson) {
        this.boxesJson = boxesJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

