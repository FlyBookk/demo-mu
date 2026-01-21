package com.musheng.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.system.dto.RoleCreateRequest;
import com.musheng.system.dto.RoleUpdateRequest;
import com.musheng.system.entity.Role;
import com.musheng.system.mapper.RoleMapper;
import com.musheng.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Role Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<Role> list(String roleCode, String roleName, Integer status, int page, int size) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(roleCode)) {
            wrapper.like(Role::getRoleCode, roleCode);
        }
        if (StringUtils.hasText(roleName)) {
            wrapper.like(Role::getRoleName, roleName);
        }
        if (status != null) {
            wrapper.eq(Role::getStatus, status);
        }

        wrapper.orderByAsc(Role::getRoleCode);

        return roleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public List<Role> getAllEnabled() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1);
        wrapper.orderByAsc(Role::getRoleCode);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public Role getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Role not found");
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role create(RoleCreateRequest request) {
        // Check role code uniqueness
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleCode, request.getRoleCode());
        if (roleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "Role code already exists");
        }

        Role role = new Role();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRoleDesc(request.getRoleDesc());
        role.setStatus(1); // Default enabled

        // Set permissions as JSON array
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            try {
                role.setPermissions(objectMapper.writeValueAsString(request.getPermissions()));
            } catch (JsonProcessingException e) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid permissions format");
            }
        } else {
            role.setPermissions("[]");
        }

        roleMapper.insert(role);
        log.info("Created role: id={}, roleCode={}", role.getId(), role.getRoleCode());

        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Role update(Long id, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Role not found");
        }

        if (StringUtils.hasText(request.getRoleName())) {
            role.setRoleName(request.getRoleName());
        }
        if (StringUtils.hasText(request.getRoleDesc())) {
            role.setRoleDesc(request.getRoleDesc());
        }
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }

        roleMapper.updateById(role);
        log.info("Updated role: id={}", id);

        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Role not found");
        }

        // Check if role is in use
        // TODO: Check if any users are assigned to this role

        roleMapper.deleteById(id);
        log.info("Deleted role: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long id, List<String> permissions) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Role not found");
        }

        try {
            role.setPermissions(objectMapper.writeValueAsString(permissions != null ? permissions : new ArrayList<>()));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Invalid permissions format");
        }

        roleMapper.updateById(role);
        log.info("Assigned permissions to role: id={}, permissionCount={}", id, permissions != null ? permissions.size() : 0);
    }

    @Override
    public List<String> getPermissions(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Role not found");
        }

        if (!StringUtils.hasText(role.getPermissions())) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse permissions JSON for role: id={}", id, e);
            return new ArrayList<>();
        }
    }
}
