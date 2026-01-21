package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Change Password Request DTO
 */
@Data
@Schema(description = "Change Password Request")
public class ChangePasswordRequest {

    @NotBlank(message = "Old password is required")
    @Schema(description = "Old password", example = "oldPassword123")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 20, message = "Password must be 6-20 characters")
    @Schema(description = "New password", example = "newPassword123")
    private String newPassword;

    @Schema(description = "Confirm new password", example = "newPassword123")
    private String confirmPassword;
}
