package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User Create Request DTO
 */
@Data
@Schema(description = "User Create Request")
public class UserCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Username
     */
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Schema(description = "Username", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * Password (optional, will use default if not provided)
     */
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    @Schema(description = "Password (optional, default: 123456)", example = "password123")
    private String password;

    /**
     * Real name
     */
    @NotBlank(message = "Real name is required")
    @Size(max = 50, message = "Real name must not exceed 50 characters")
    @Schema(description = "Real name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String realName;

    /**
     * Email
     */
    @Email(message = "Invalid email format")
    @Schema(description = "Email", example = "john@example.com")
    private String email;

    /**
     * Phone number
     */
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    @Schema(description = "Phone number", example = "+86 13800138000")
    private String phone;

    /**
     * Role code
     */
    @NotBlank(message = "Role code is required")
    @Schema(description = "Role code", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;
}
