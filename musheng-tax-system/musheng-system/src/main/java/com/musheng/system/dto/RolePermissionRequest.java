package com.musheng.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Role Permission Request DTO
 */
@Data
@Schema(description = "Role Permission Request")
public class RolePermissionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Permission codes
     */
    @NotNull(message = "Permissions list is required")
    @Schema(description = "Permission codes", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> permissions;
}
