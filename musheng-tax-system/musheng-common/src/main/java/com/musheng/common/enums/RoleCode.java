package com.musheng.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色编码枚举
 */
@Getter
@AllArgsConstructor
public enum RoleCode {

    ADMIN("admin", "管理员"),
    FINANCE("finance", "财务人员"),
    VIEWER("viewer", "查看者");

    private final String code;
    private final String description;

    public static RoleCode fromCode(String code) {
        for (RoleCode role : values()) {
            if (role.getCode().equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }
}
