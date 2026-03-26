package com.zhou.controller;

import com.zhou.dto.AnnotationSaveRequest;
import com.zhou.model.ImageAnnotation;
import com.zhou.service.AnnotationService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/annotations")
public class AnnotationController {
    private final AnnotationService annotationService;

    public AnnotationController(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageAnnotation uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "batchId", required = false) String batchId,
            @RequestParam(value = "relativePath", required = false) String relativePath
    ) throws IOException {
        return annotationService.uploadImage(file, batchId, relativePath);
    }

    @GetMapping("/images")
    public List<ImageAnnotation> listImages(@RequestParam(value = "batchId", required = false) String batchId) throws IOException {
        return annotationService.listImages(batchId);
    }

    @GetMapping("/images/{imageId}")
    public ImageAnnotation getImage(@PathVariable String imageId) throws IOException {
        return annotationService.getById(imageId);
    }

    @GetMapping("/images/{imageId}/content")
    public ResponseEntity<Resource> getImageContent(@PathVariable String imageId) throws IOException {
        ImageAnnotation annotation = annotationService.getById(imageId);
        Path imagePath = Path.of(annotation.getStoredPath());
        Resource resource = new FileSystemResource(imagePath);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    @DeleteMapping("/images")
    public ResponseEntity<Void> clearImages() throws IOException {
        annotationService.clearAllImages();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable String imageId) throws IOException {
        annotationService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/images/{imageId}/boxes")
    public ImageAnnotation saveBoxes(@PathVariable String imageId, @Valid @RequestBody AnnotationSaveRequest request) throws IOException {
        return annotationService.updateBoxes(imageId, request.boxes());
    }
}
