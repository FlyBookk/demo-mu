package com.musheng.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.system.dto.UserCreateRequest;
import com.musheng.system.dto.UserUpdateRequest;
import com.musheng.system.entity.User;

/**
 * User Service Interface
 */
public interface UserService {

    /**
     * Query users with pagination
     *
     * @param username Username (optional)
     * @param realName Real name (optional)
     * @param roleCode Role code (optional)
     * @param status   Status (optional)
     * @param page     Page number
     * @param size     Page size
     * @return Paginated result
     */
    Page<User> list(String username, String realName, String roleCode, Integer status, int page, int size);

    /**
     * Get user by ID
     *
     * @param id User ID
     * @return User entity
     */
    User getById(Long id);

    /**
     * Create a new user
     *
     * @param request Create request
     * @return Created user
     */
    User create(UserCreateRequest request);

    /**
     * Update existing user
     *
     * @param id      User ID
     * @param request Update request
     * @return Updated user
     */
    User update(Long id, UserUpdateRequest request);

    /**
     * Delete user
     *
     * @param id User ID
     */
    void delete(Long id);

    /**
     * Reset user password to default
     *
     * @param id User ID
     */
    void resetPassword(Long id);

    /**
     * Toggle user status (enable/disable)
     *
     * @param id User ID
     */
    void toggleStatus(Long id);

    /**
     * Enable user
     *
     * @param id User ID
     */
    void enable(Long id);

    /**
     * Disable user
     *
     * @param id User ID
     */
    void disable(Long id);
}
