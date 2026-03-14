package com.musheng.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.system.dto.LoginRequest;
import com.musheng.system.dto.LoginResponse;
import com.musheng.system.dto.UpdateProfileRequest;
import com.musheng.system.entity.Role;
import com.musheng.system.entity.User;
import com.musheng.system.mapper.RoleMapper;
import com.musheng.system.mapper.UserMapper;
import com.musheng.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Authentication Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Maximum login failure count before locking
     */
    private static final int MAX_FAIL_COUNT = 5;

    /**
     * Lock duration in minutes
     */
    private static final int LOCK_MINUTES = 30;

    /**
     * Token expiration time in seconds (24 hours)
     */
    private static final long TOKEN_EXPIRE_SECONDS = 86400;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, String clientIp) {
        // 1. Find user by username
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            log.warn("Login failed: user not found - {}", request.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 2. Check if account is disabled
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 3. Check if account is locked
        if (user.getLockTime() != null &&
                user.getLockTime().plusMinutes(LOCK_MINUTES).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "Account locked, please try again after " + LOCK_MINUTES + " minutes");
        }

        // 4. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Increase fail count
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);

            if (failCount >= MAX_FAIL_COUNT) {
                user.setLockTime(LocalDateTime.now());
                log.warn("Account locked due to too many failed attempts: {}", user.getUsername());
            }

            userMapper.updateById(user);
            log.warn("Login failed: wrong password - {}", request.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 5. Login successful - reset fail count and update login info
        user.setLoginFailCount(0);
        user.setLockTime(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        // 6. Generate token using Sa-Token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        // 7. Get role and permissions
        Role role = roleMapper.selectByCode(user.getRoleCode());
        List<String> permissions = parsePermissions(role);

        log.info("User logged in successfully: {}", user.getUsername());

        // 8. Build response (P0: include permissions)
        // Fixed: Changed 'user' to 'userInfo' for frontend compatibility
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(TOKEN_EXPIRE_SECONDS)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .role(user.getRoleCode())  // Added for frontend compatibility
                        .roleCode(user.getRoleCode())
                        .roleName(role != null ? role.getRoleName() : null)
                        .avatar(user.getAvatar())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .status(user.getStatus())
                        .createTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null)
                        .lastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null)
                        .build())
                .permissions(permissions)
                .build();
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            log.info("User logged out: {}", StpUtil.getLoginId());
            StpUtil.logout();
        }
    }

    @Override
    public LoginResponse getCurrentUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Role role = roleMapper.selectByCode(user.getRoleCode());
        List<String> permissions = parsePermissions(role);

        // Fixed: Changed 'user' to 'userInfo' for frontend compatibility
        return LoginResponse.builder()
                .token(StpUtil.getTokenValue())
                .tokenType("Bearer")
                .expiresIn(TOKEN_EXPIRE_SECONDS)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .role(user.getRoleCode())  // Added for frontend compatibility
                        .roleCode(user.getRoleCode())
                        .roleName(role != null ? role.getRoleName() : null)
                        .avatar(user.getAvatar())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .status(user.getStatus())
                        .createTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null)
                        .lastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null)
                        .build())
                .permissions(permissions)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateProfileRequest request) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 更新个人信息
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        userMapper.updateById(user);

        log.info("用户更新个人信息: {}", user.getUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String oldPassword, String newPassword) {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "Old password is incorrect");
        }

        // Update new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);

        log.info("User changed password: {}", user.getUsername());
    }

    /**
     * Parse permissions from role
     */
    private List<String> parsePermissions(Role role) {
        if (role == null || role.getPermissions() == null) {
            return Collections.emptyList();
        }

        try {
            JSONArray array = JSONUtil.parseArray(role.getPermissions());
            return array.toList(String.class);
        } catch (Exception e) {
            log.warn("Failed to parse permissions: {}", role.getPermissions());
            return Collections.emptyList();
        }
    }

    @Override
    public LoginResponse refreshToken() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // Renew token
        StpUtil.renewTimeout(TOKEN_EXPIRE_SECONDS);
        String newToken = StpUtil.getTokenValue();

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Role role = roleMapper.selectByCode(user.getRoleCode());
        List<String> permissions = parsePermissions(role);

        log.info("Token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .token(newToken)
                .tokenType("Bearer")
                .expiresIn(TOKEN_EXPIRE_SECONDS)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .role(user.getRoleCode())
                        .roleCode(user.getRoleCode())
                        .roleName(role != null ? role.getRoleName() : null)
                        .avatar(user.getAvatar())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .status(user.getStatus())
                        .createTime(user.getCreateTime() != null ? user.getCreateTime().toString() : null)
                        .lastLoginTime(user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : null)
                        .build())
                .permissions(permissions)
                .build();
    }

    @Override
    public List<String> getCurrentPermissions() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Role role = roleMapper.selectByCode(user.getRoleCode());
        return parsePermissions(role);
    }
}
