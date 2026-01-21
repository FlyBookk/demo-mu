package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * Role Update Request DTO
 */
@Data
@Schema(description = "Role Update Request")
public class RoleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Role name
     */
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    @Schema(description = "Role name", example = "Administrator")
    private String roleName;

    /**
     * Role description
     */
    @Size(max = 255, message = "Role description must not exceed 255 characters")
    @Schema(description = "Role description", example = "System administrator with full access")
    private String roleDesc;

    /**
     * Status (1-enabled, 0-disabled)
     */
    @Schema(description = "Status (1-enabled, 0-disabled)", example = "1")
    private Integer status;
}
