package com.zhou.model;

import java.time.Instant;

public class DatasetBuildRecordEntity {
    private String recordId;
    private String datasetName;
    private String datasetPath;
    private String outputPreset;
    private Integer trainCount;
    private Integer valCount;
    private Integer testCount;
    private Double valRatio;
    private Double testRatio;
    private Instant createdAt;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getDatasetPath() {
        return datasetPath;
    }

    public void setDatasetPath(String datasetPath) {
        this.datasetPath = datasetPath;
    }

    public String getOutputPreset() {
        return outputPreset;
    }

    public void setOutputPreset(String outputPreset) {
        this.outputPreset = outputPreset;
    }

    public Integer getTrainCount() {
        return trainCount;
    }

    public void setTrainCount(Integer trainCount) {
        this.trainCount = trainCount;
    }

    public Integer getValCount() {
        return valCount;
    }

    public void setValCount(Integer valCount) {
        this.valCount = valCount;
    }

    public Integer getTestCount() {
        return testCount;
    }

    public void setTestCount(Integer testCount) {
        this.testCount = testCount;
    }

    public Double getValRatio() {
        return valRatio;
    }

    public void setValRatio(Double valRatio) {
        this.valRatio = valRatio;
    }

    public Double getTestRatio() {
        return testRatio;
    }

    public void setTestRatio(Double testRatio) {
        this.testRatio = testRatio;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
