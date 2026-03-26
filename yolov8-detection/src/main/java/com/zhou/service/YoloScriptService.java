package com.zhou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhou.config.YoloProperties;
import com.zhou.model.BoundingBox;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoloScriptService {
    private final YoloProperties properties;
    private final PythonProcessRunner processRunner;
    private final ObjectMapper objectMapper;
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("__PROGRESS__:(\\d{1,3})");
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\u001B\\[[;\\d]*[ -/]*[@-~]");

    public YoloScriptService(YoloProperties properties, PythonProcessRunner processRunner, ObjectMapper objectMapper) {
        this.properties = properties;
        this.processRunner = processRunner;
        this.objectMapper = objectMapper;
    }

    public List<BoundingBox> detect(Path imagePath, String modelPath, Double conf, Double iou, String device) throws IOException, InterruptedException {
        return detect(imagePath, modelPath, conf, iou, device, null, null, null);
    }

    public List<BoundingBox> detect(
            Path imagePath,
            String modelPath,
            Double conf,
            Double iou,
            String device,
            Integer imgsz,
            Boolean augment,
            Integer maxDet
    ) throws IOException, InterruptedException {
        int finalImgsz = imgsz == null ? 960 : Math.max(320, imgsz);
        int finalMaxDet = maxDet == null ? 300 : Math.max(1, maxDet);
        double finalConf = conf == null ? properties.getConf() : conf;
        double finalIou = iou == null ? properties.getIou() : iou;
        String finalModelPath = resolvePath(fallback(modelPath, properties.getModelPath()));
        String finalDevice = fallback(device, properties.getDevice());

        List<BoundingBox> primary = runDetectOnce(
                imagePath,
                finalModelPath,
                finalConf,
                finalIou,
                finalDevice,
                finalImgsz,
                Boolean.TRUE.equals(augment),
                finalMaxDet
        );
        if (!primary.isEmpty() || finalConf <= 0.05d) {
            return primary;
        }

        // 首次无检出时降阈值兜底，提升低质量图片召回率。
        double fallbackConf = Math.max(0.03d, Math.min(0.08d, finalConf * 0.5d));
        return runDetectOnce(
                imagePath,
                finalModelPath,
                fallbackConf,
                finalIou,
                finalDevice,
                finalImgsz,
                true,
                finalMaxDet
        );
    }

    private List<BoundingBox> runDetectOnce(
            Path imagePath,
            String modelPath,
            double conf,
            double iou,
            String device,
            int imgsz,
            boolean augment,
            int maxDet
    ) throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>(List.of(
                properties.getPythonCommand(),
                resolvePath(properties.getDetectScript()),
                "detect",
                "--image", imagePath.toString(),
                "--model", modelPath,
                "--conf", String.valueOf(conf),
                "--iou", String.valueOf(iou),
                "--device", device,
                "--imgsz", String.valueOf(imgsz),
                "--max-det", String.valueOf(maxDet)
        ));
        if (augment) {
            cmd.add("--augment");
        }

        try {
            String output = processRunner.run(cmd, Duration.ofMinutes(4));
            return objectMapper.readValue(output, new TypeReference<>() {
            });
        } catch (IllegalStateException ex) {
            if (shouldFallbackToCpu(device, ex)) {
                List<String> fallbackCmd = new java.util.ArrayList<>(cmd);
                int deviceIndex = fallbackCmd.indexOf("--device");
                if (deviceIndex >= 0 && deviceIndex + 1 < fallbackCmd.size()) {
                    fallbackCmd.set(deviceIndex + 1, "cpu");
                }
                String output = processRunner.run(fallbackCmd, Duration.ofMinutes(4));
                return objectMapper.readValue(output, new TypeReference<>() {
                });
            }
            throw ex;
        }
    }

    public Map<String, Object> train(Path datasetPath, int epochs, int batch, String modelPath) throws IOException, InterruptedException {
        return train(datasetPath, epochs, batch, modelPath, null, null);
    }

    public Map<String, Object> train(
            Path datasetPath,
            int epochs,
            int batch,
            String modelPath,
            Double conf,
            IntConsumer progressConsumer
    ) throws IOException, InterruptedException {
        String finalModelPath = resolvePath(fallback(modelPath, properties.getModelPath()));
        double finalConf = conf == null ? properties.getConf() : conf;
        List<String> cmd = List.of(
                properties.getPythonCommand(),
                resolvePath(properties.getTrainScript()),
                "train",
                "--dataset", datasetPath.toString(),
                "--epochs", String.valueOf(epochs),
                "--batch", String.valueOf(batch),
                "--model", finalModelPath,
                "--conf", String.valueOf(finalConf),
                "--device", properties.getDevice()
        );
        String output = processRunner.run(cmd, Duration.ofMinutes(60), line -> {
            if (progressConsumer == null || line == null) {
                return;
            }
            String normalized = ANSI_ESCAPE_PATTERN.matcher(line).replaceAll("").trim();
            Matcher matcher = PROGRESS_PATTERN.matcher(normalized);
            if (!matcher.find()) {
                return;
            }
            int progress = Integer.parseInt(matcher.group(1));
            progressConsumer.accept(Math.max(0, Math.min(100, progress)));
        });
        Map<String, Object> result = objectMapper.readValue(output, new TypeReference<>() {
        });
        Map<String, Object> merged = new HashMap<>(result);
        merged.put("datasetPath", datasetPath.toString());
        merged.put("modelPath", finalModelPath);
        merged.put("conf", finalConf);
        return merged;
    }

    private String resolvePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        Path configured = Path.of(raw);
        if (configured.isAbsolute()) {
            return configured.normalize().toString();
        }

        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path moduleRoot = cwd.resolve("yolov8-detection").normalize();

        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve(configured).normalize());
        candidates.add(moduleRoot.resolve(configured).normalize());

        // Filename-only input from browser file picker should still map to configured model directory.
        if (configured.getNameCount() == 1 && properties.getModelPath() != null && !properties.getModelPath().isBlank()) {
            Path defaultModel = Path.of(properties.getModelPath());
            Path defaultParent = defaultModel.getParent();
            if (defaultParent != null) {
                if (defaultModel.isAbsolute()) {
                    candidates.add(defaultParent.resolve(configured).normalize());
                } else {
                    candidates.add(cwd.resolve(defaultParent).resolve(configured).normalize());
                    candidates.add(moduleRoot.resolve(defaultParent).resolve(configured).normalize());
                }
            }
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toString();
            }
        }

        return candidates.get(0).toString();
    }

    private String fallback(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private boolean shouldFallbackToCpu(String requestedDevice, IllegalStateException ex) {
        if (requestedDevice == null || requestedDevice.isBlank()) {
            return false;
        }
        String normalizedDevice = requestedDevice.trim().toLowerCase(Locale.ROOT);
        if ("cpu".equals(normalizedDevice)) {
            return false;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("cuda")
                || normalizedMessage.contains("nvidia")
                || normalizedMessage.contains("device")
                || normalizedMessage.contains("invalid argument 'device'")
                || normalizedMessage.contains("torch not compiled with cuda");
    }
}
