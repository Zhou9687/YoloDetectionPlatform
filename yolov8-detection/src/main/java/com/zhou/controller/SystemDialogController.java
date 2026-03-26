package com.zhou.controller;

import com.zhou.service.NativeDialogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemDialogController {
    private final NativeDialogService nativeDialogService;

    @Value("${server.port:8081}")
    private String serverPort;

    public SystemDialogController(NativeDialogService nativeDialogService) {
        this.nativeDialogService = nativeDialogService;
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of(
                "service", "yolo-detection",
                "apiVersion", "2026-03-25-startup-check-v1",
                "serverPort", serverPort,
                "time", Instant.now().toString(),
                "capabilities", List.of(
                        "auth.login",
                        "auth.register",
                        "auth.profile.username",
                        "auth.profile.password",
                        "system.pick-directory",
                        "system.pick-file"
                )
        );
    }

    @PostMapping("/pick-directory")
    public Map<String, String> pickDirectory(@RequestBody(required = false) PickDirectoryRequest request) {
        String title = request == null ? null : request.title();
        String path = nativeDialogService.pickDirectory(title);
        return Map.of("path", path == null ? "" : path);
    }

    @PostMapping("/pick-file")
    public Map<String, String> pickFile(@RequestBody(required = false) PickFileRequest request) {
        String title = request == null ? null : request.title();
        List<String> extensions = request == null ? List.of() : request.extensions();
        String path = nativeDialogService.pickFile(title, extensions);
        return Map.of("path", path == null ? "" : path);
    }

    public record PickDirectoryRequest(String title) {
    }

    public record PickFileRequest(String title, List<String> extensions) {
    }
}
