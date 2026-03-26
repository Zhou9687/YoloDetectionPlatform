package com.zhou.controller;

import com.zhou.dto.AuthResponse;
import com.zhou.dto.LoginRequest;
import com.zhou.dto.RegisterRequest;
import com.zhou.dto.UpdatePasswordRequest;
import com.zhou.dto.UpdateUsernameRequest;
import com.zhou.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request.username(), request.password());
        return Map.of("message", "注册成功");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/profile/username")
    public Map<String, String> updateUsername(@Valid @RequestBody UpdateUsernameRequest request) {
        String updated = authService.updateUsername(request.currentUsername(), request.newUsername());
        return Map.of("username", updated, "message", "账号名称修改成功");
    }

    @PostMapping("/profile/password")
    public Map<String, String> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        authService.updatePassword(request.username(), request.oldPassword(), request.newPassword());
        return Map.of("message", "密码修改成功");
    }

    @PostMapping("/update-username")
    public Map<String, String> updateUsernameLegacy(@RequestBody Map<String, String> body) {
        String current = valueOrEmpty(body.get("currentUsername"));
        if (current.isBlank()) {
            current = valueOrEmpty(body.get("username"));
        }
        String next = valueOrEmpty(body.get("newUsername"));
        String updated = authService.updateUsername(current, next);
        return Map.of("username", updated, "message", "账号名称修改成功");
    }

    @PostMapping("/update-password")
    public Map<String, String> updatePasswordLegacy(@RequestBody Map<String, String> body) {
        String username = valueOrEmpty(body.get("username"));
        String oldPassword = valueOrEmpty(body.get("oldPassword"));
        String newPassword = valueOrEmpty(body.get("newPassword"));
        authService.updatePassword(username, oldPassword, newPassword);
        return Map.of("message", "密码修改成功");
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
