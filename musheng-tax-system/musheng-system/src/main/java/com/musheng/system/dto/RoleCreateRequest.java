package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Role Create Request DTO
 */
@Data
@Schema(description = "Role Create Request")
public class RoleCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Role code
     */
    @NotBlank(message = "Role code is required")
    @Size(max = 50, message = "Role code must not exceed 50 characters")
    @Schema(description = "Role code", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    /**
     * Role name
     */
    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    @Schema(description = "Role name", example = "Administrator", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleName;

    /**
     * Role description
     */
    @Size(max = 255, message = "Role description must not exceed 255 characters")
    @Schema(description = "Role description", example = "System administrator with full access")
    private String roleDesc;

    /**
     * Permission codes
     */
    @Schema(description = "Permission codes")
    private List<String> permissions;
}
