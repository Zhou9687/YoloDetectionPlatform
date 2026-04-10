package com.zhou.model;

import java.time.Instant;

public class TrainingJob {
    private String jobId;
    private String datasetPath;
    private String modelPath;
    private int epochs;
    private int batch;
    private Double conf;
    private int progress;
    private TrainingJobStatus status;
    private Instant createdAt;
    private Instant finishedAt;
    private String message;
    private String bestModelPath;
    private Double precision;
    private Double recall;
    private Double map50;
    private Double map95;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getDatasetPath() {
        return datasetPath;
    }

    public void setDatasetPath(String datasetPath) {
        this.datasetPath = datasetPath;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public int getEpochs() {
        return epochs;
    }

    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public Double getConf() {
        return conf;
    }

    public void setConf(Double conf) {
        this.conf = conf;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public TrainingJobStatus getStatus() {
        return status;
    }

    public void setStatus(TrainingJobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBestModelPath() {
        return bestModelPath;
    }

    public void setBestModelPath(String bestModelPath) {
        this.bestModelPath = bestModelPath;
    }

    public Double getPrecision() {
        return precision;
    }

    public void setPrecision(Double precision) {
        this.precision = precision;
    }

    public Double getRecall() {
        return recall;
    }

    public void setRecall(Double recall) {
        this.recall = recall;
    }

    public Double getMap50() {
        return map50;
    }

    public void setMap50(Double map50) {
        this.map50 = map50;
    }

    public Double getMap95() {
        return map95;
    }

    public void setMap95(Double map95) {
        this.map95 = map95;
    }
}
