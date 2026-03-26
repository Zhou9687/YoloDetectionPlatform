package com.zhou.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhou.model.BoundingBox;
import com.zhou.model.PredictionRecordEntity;
import com.zhou.repository.PredictionRecordRepository;
import com.zhou.service.StorageService;
import com.zhou.service.YoloScriptService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {
    private final YoloScriptService yoloScriptService;
    private final StorageService storageService;
    private final PredictionRecordRepository predictionRecordRepository;
    private final ObjectMapper objectMapper;

    public DetectionController(
            YoloScriptService yoloScriptService,
            StorageService storageService,
            PredictionRecordRepository predictionRecordRepository,
            ObjectMapper objectMapper
    ) {
        this.yoloScriptService = yoloScriptService;
        this.storageService = storageService;
        this.predictionRecordRepository = predictionRecordRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Map<String, Object>> predict(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "paths", required = false) List<String> paths,
            @RequestParam(value = "modelPath", required = false) String modelPath,
            @RequestParam(value = "conf", required = false) Double conf,
            @RequestParam(value = "iou", required = false) Double iou,
            @RequestParam(value = "device", required = false) String device,
            @RequestParam(value = "imgsz", required = false) Integer imgsz,
            @RequestParam(value = "augment", required = false) Boolean augment,
            @RequestParam(value = "maxDet", required = false) Integer maxDet
    ) throws IOException, InterruptedException {
        List<Map<String, Object>> response = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String relativePath = paths != null && i < paths.size() ? paths.get(i) : file.getOriginalFilename();
            Path tempFile = Files.createTempFile("predict-", suffix(file.getOriginalFilename()));
            try {
                file.transferTo(tempFile);
                List<BoundingBox> boxes = yoloScriptService.detect(tempFile, modelPath, conf, iou, device, imgsz, augment, maxDet);
                String resultId = UUID.randomUUID().toString();
                Path resultPath = storageService.predictResultsDir().resolve(resultId + ".png");
                renderPrediction(tempFile, boxes, resultPath);

                persistPrediction(resultId, file.getOriginalFilename(), relativePath, resultPath, modelPath, conf, iou, device, imgsz, augment, maxDet, boxes);

                Map<String, Object> item = new HashMap<>();
                item.put("success", true);
                item.put("fileName", file.getOriginalFilename());
                item.put("relativePath", relativePath);
                item.put("boxes", boxes);
                item.put("resultId", resultId);
                item.put("resultImageUrl", "/api/detection/results/" + resultId);
                response.add(item);
            } catch (Exception ex) {
                Map<String, Object> item = new HashMap<>();
                item.put("success", false);
                item.put("fileName", file.getOriginalFilename());
                item.put("relativePath", relativePath);
                item.put("error", safeMessage(ex));
                response.add(item);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
        return response;
    }

    @GetMapping("/results/{resultId}")
    public ResponseEntity<Resource> getResultImage(@PathVariable String resultId) {
        Path path = storageService.predictResultsDir().resolve(resultId + ".png").toAbsolutePath().normalize();
        if (!path.startsWith(storageService.predictResultsDir().toAbsolutePath().normalize()) || !Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(path);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.IMAGE_PNG);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    private void persistPrediction(
            String resultId,
            String fileName,
            String relativePath,
            Path resultPath,
            String modelPath,
            Double conf,
            Double iou,
            String device,
            Integer imgsz,
            Boolean augment,
            Integer maxDet,
            List<BoundingBox> boxes
    ) {
        PredictionRecordEntity entity = new PredictionRecordEntity();
        entity.setResultId(resultId);
        entity.setFileName(fileName);
        entity.setRelativePath(relativePath);
        entity.setResultImagePath(resultPath.toAbsolutePath().normalize().toString());
        entity.setModelPath(modelPath);
        entity.setConf(conf);
        entity.setIou(iou);
        entity.setDevice(device);
        entity.setImgsz(imgsz);
        entity.setAugment(Boolean.TRUE.equals(augment) ? 1 : 0);
        entity.setMaxDet(maxDet);
        entity.setBoxesJson(toJson(boxes));
        entity.setCreatedAt(Instant.now());
        predictionRecordRepository.save(entity);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            return "[]";
        }
    }

    private void renderPrediction(Path imagePath, List<BoundingBox> boxes, Path resultPath) throws IOException {
        BufferedImage source = ImageIO.read(imagePath.toFile());
        if (source == null) {
            throw new IOException("Unsupported image format: " + imagePath.getFileName());
        }
        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setStroke(new BasicStroke(2.0f));
        g.setFont(new Font("Arial", Font.PLAIN, 14));

        for (BoundingBox box : boxes) {
            int w = (int) Math.round(box.width() * source.getWidth());
            int h = (int) Math.round(box.height() * source.getHeight());
            int x = (int) Math.round((box.x() - box.width() / 2.0) * source.getWidth());
            int y = (int) Math.round((box.y() - box.height() / 2.0) * source.getHeight());
            g.setColor(new Color(239, 68, 68));
            g.drawRect(x, y, Math.max(w, 1), Math.max(h, 1));
            String text = box.label() + " " + String.format(java.util.Locale.US, "%.2f", box.confidence());
            int textY = Math.max(14, y - 4);
            g.fillRect(x, textY - 14, Math.max(80, text.length() * 8), 16);
            g.setColor(Color.WHITE);
            g.drawString(text, x + 3, textY - 2);
        }
        g.dispose();
        ImageIO.write(output, "png", resultPath.toFile());
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "prediction failed";
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() > 300) {
            return normalized.substring(0, 300) + "...";
        }
        if (normalized.toLowerCase(Locale.ROOT).contains("timed out")) {
            return "prediction timed out";
        }
        return normalized;
    }

    private String suffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}
