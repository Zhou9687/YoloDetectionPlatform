package com.zhou.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "yolo")
public class YoloProperties {
    private String workspace = "data";
    private String pythonCommand = "python";
    private String detectScript = "scripts/mock_yolo.py";
    private String trainScript = "scripts/mock_yolo.py";
    private String modelPath = "D:/ProjectCode/IDEA/yolo/yolov8n.pt";
    private double conf = 0.12;
    private double iou = 0.45;
    private String device = "cpu";
    private List<String> datasetSearchRoots = new ArrayList<>();

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getPythonCommand() {
        return pythonCommand;
    }

    public void setPythonCommand(String pythonCommand) {
        this.pythonCommand = pythonCommand;
    }

    public String getDetectScript() {
        return detectScript;
    }

    public void setDetectScript(String detectScript) {
        this.detectScript = detectScript;
    }

    public String getTrainScript() {
        return trainScript;
    }

    public void setTrainScript(String trainScript) {
        this.trainScript = trainScript;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public double getConf() {
        return conf;
    }

    public void setConf(double conf) {
        this.conf = conf;
    }

    public double getIou() {
        return iou;
    }

    public void setIou(double iou) {
        this.iou = iou;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public List<String> getDatasetSearchRoots() {
        return datasetSearchRoots;
    }

    public void setDatasetSearchRoots(List<String> datasetSearchRoots) {
        this.datasetSearchRoots = datasetSearchRoots == null ? new ArrayList<>() : datasetSearchRoots;
    }
}
