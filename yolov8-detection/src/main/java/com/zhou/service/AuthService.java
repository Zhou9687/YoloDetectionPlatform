package com.zhou.service;

import com.zhou.dto.AuthResponse;
import com.zhou.model.UserAccountEntity;
import com.zhou.repository.UserAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public void register(String username, String password) {
        String cleanUsername = normalize(username);
        if (userAccountRepository.findByUsername(cleanUsername) != null) {
            throw new IllegalArgumentException("账号已存在");
        }

        UserAccountEntity entity = new UserAccountEntity();
        entity.setUserId(UUID.randomUUID().toString());
        entity.setUsername(cleanUsername);
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setCreatedAt(Instant.now());
        userAccountRepository.save(entity);
    }

    public AuthResponse login(String username, String password) {
        String cleanUsername = normalize(username);
        UserAccountEntity entity = userAccountRepository.findByUsername(cleanUsername);
        if (entity == null || !passwordEncoder.matches(password, entity.getPasswordHash())) {
            throw new SecurityException("账号或密码错误");
        }
        String token = UUID.randomUUID().toString();
        return new AuthResponse(token, entity.getUsername());
    }

    public String updateUsername(String currentUsername, String newUsername) {
        String cleanCurrent = normalize(currentUsername);
        String cleanNew = normalize(newUsername);
        if (cleanCurrent.equals(cleanNew)) {
            return cleanCurrent;
        }

        UserAccountEntity current = userAccountRepository.findByUsername(cleanCurrent);
        if (current == null) {
            throw new SecurityException("账号不存在或登录已失效");
        }
        UserAccountEntity duplicated = userAccountRepository.findByUsername(cleanNew);
        if (duplicated != null) {
            throw new IllegalArgumentException("新账号名称已存在");
        }

        userAccountRepository.updateUsername(current.getUserId(), cleanNew);
        return cleanNew;
    }

    public void updatePassword(String username, String oldPassword, String newPassword) {
        String cleanUsername = normalize(username);
        UserAccountEntity current = userAccountRepository.findByUsername(cleanUsername);
        if (current == null) {
            throw new SecurityException("账号不存在或登录已失效");
        }
        if (!passwordEncoder.matches(oldPassword, current.getPasswordHash())) {
            throw new SecurityException("原密码错误");
        }

        String encoded = passwordEncoder.encode(newPassword);
        userAccountRepository.updatePasswordHash(current.getUserId(), encoded);
    }

    private String normalize(String username) {
        return (username == null ? "" : username).trim();
    }
}
