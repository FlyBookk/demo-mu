package com.musheng.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.system.dto.RoleCreateRequest;
import com.musheng.system.dto.RoleUpdateRequest;
import com.musheng.system.entity.Role;

import java.util.List;

/**
 * Role Service Interface
 */
public interface RoleService {

    /**
     * Query roles with pagination
     *
     * @param roleCode Role code (optional)
     * @param roleName Role name (optional)
     * @param status   Status (optional)
     * @param page     Page number
     * @param size     Page size
     * @return Paginated result
     */
    Page<Role> list(String roleCode, String roleName, Integer status, int page, int size);

    /**
     * Get all enabled roles
     *
     * @return List of enabled roles
     */
    List<Role> getAllEnabled();

    /**
     * Get role by ID
     *
     * @param id Role ID
     * @return Role entity
     */
    Role getById(Long id);

    /**
     * Create a new role
     *
     * @param request Create request
     * @return Created role
     */
    Role create(RoleCreateRequest request);

    /**
     * Update existing role
     *
     * @param id      Role ID
     * @param request Update request
     * @return Updated role
     */
    Role update(Long id, RoleUpdateRequest request);

    /**
     * Delete role
     *
     * @param id Role ID
     */
    void delete(Long id);

    /**
     * Assign permissions to role
     *
     * @param id          Role ID
     * @param permissions List of permission codes
     */
    void assignPermissions(Long id, List<String> permissions);

    /**
     * Get permissions of a role
     *
     * @param id Role ID
     * @return List of permission codes
     */
    List<String> getPermissions(Long id);
}
