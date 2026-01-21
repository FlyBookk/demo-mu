package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * User Update Request DTO
 */
@Data
@Schema(description = "User Update Request")
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Real name
     */
    @Size(max = 50, message = "Real name must not exceed 50 characters")
    @Schema(description = "Real name", example = "John Doe")
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
     * Avatar URL
     */
    @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
    @Schema(description = "Avatar URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /**
     * Role code
     */
    @Schema(description = "Role code", example = "ADMIN")
    private String roleCode;
}
