package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Login Response DTO
 * P0 Requirement: Include permissions field
 * Fixed: Changed user to userInfo for frontend compatibility
 */
@Data
@Builder
@Schema(description = "Login Response")
public class LoginResponse {

    @Schema(description = "Access token")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token expiration time in seconds", example = "86400")
    private long expiresIn;

    /**
     * Fixed: Changed from 'user' to 'userInfo' for frontend compatibility
     */
    @Schema(description = "User information")
    private UserInfo userInfo;

    /**
     * P0 Requirement: Permission list
     */
    @Schema(description = "Permission list")
    private List<String> permissions;

    @Data
    @Builder
    @Schema(description = "User Information")
    public static class UserInfo {

        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "Username", example = "admin")
        private String username;

        @Schema(description = "Real name", example = "System Administrator")
        private String realName;

        /**
         * Fixed: Added 'role' field for frontend compatibility (same as roleCode)
         */
        @Schema(description = "Role (same as roleCode)", example = "admin")
        private String role;

        @Schema(description = "Role code", example = "admin")
        private String roleCode;

        @Schema(description = "Role name", example = "Administrator")
        private String roleName;

        @Schema(description = "Avatar URL")
        private String avatar;

        /**
         * Added missing fields for frontend compatibility
         */
        @Schema(description = "Email address")
        private String email;

        @Schema(description = "Phone number")
        private String phone;

        @Schema(description = "User status: 1-active, 0-disabled")
        private Integer status;

        @Schema(description = "Create time")
        private String createTime;

        @Schema(description = "Last login time")
        private String lastLoginTime;
    }
}

