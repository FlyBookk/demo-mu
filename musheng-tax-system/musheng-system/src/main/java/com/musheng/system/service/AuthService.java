package com.musheng.system.service;

import com.musheng.system.dto.LoginRequest;
import com.musheng.system.dto.LoginResponse;

import java.util.List;

/**
 * Authentication Service Interface
 */
public interface AuthService {

    /**
     * User login
     *
     * @param request Login request
     * @param clientIp Client IP address
     * @return Login response with token and permissions
     */
    LoginResponse login(LoginRequest request, String clientIp);

    /**
     * User logout
     */
    void logout();

    /**
     * Get current user info
     *
     * @return Login response with user info
     */
    LoginResponse getCurrentUser();

    /**
     * Change password
     *
     * @param oldPassword Old password
     * @param newPassword New password
     */
    void changePassword(String oldPassword, String newPassword);

    /**
     * Refresh token
     *
     * @return Login response with new token
     */
    LoginResponse refreshToken();

    /**
     * Get current user permissions
     *
     * @return Permission list
     */
    List<String> getCurrentPermissions();
}
