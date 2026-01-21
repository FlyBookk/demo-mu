package com.musheng.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.system.dto.UserCreateRequest;
import com.musheng.system.dto.UserUpdateRequest;
import com.musheng.system.entity.User;
import com.musheng.system.mapper.UserMapper;
import com.musheng.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * User Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public Page<User> list(String username, String realName, String roleCode, Integer status, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(User::getRealName, realName);
        }
        if (StringUtils.hasText(roleCode)) {
            wrapper.eq(User::getRoleCode, roleCode);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);

        // Clear password field for security
        result.getRecords().forEach(user -> user.setPassword(null));

        return result;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }
        // Clear password field for security
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User create(UserCreateRequest request) {
        // Check username uniqueness
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXIST, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : DEFAULT_PASSWORD));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRoleCode(request.getRoleCode());
        user.setStatus(1); // Default enabled

        if (StpUtil.isLogin()) {
            user.setCreateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.insert(user);
        log.info("Created user: id={}, username={}", user.getId(), user.getUsername());

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User update(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getRoleCode())) {
            user.setRoleCode(request.getRoleCode());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }

        if (StpUtil.isLogin()) {
            user.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.updateById(user);
        log.info("Updated user: id={}", id);

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        userMapper.deleteById(id);
        log.info("Deleted user: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setLoginFailCount(0);
        user.setLockTime(null);

        if (StpUtil.isLogin()) {
            user.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.updateById(user);
        log.info("Reset password for user: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        user.setStatus(user.getStatus() == 1 ? 0 : 1);

        if (StpUtil.isLogin()) {
            user.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.updateById(user);
        log.info("Toggled user status: id={}, newStatus={}", id, user.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setLockTime(null);

        if (StpUtil.isLogin()) {
            user.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.updateById(user);
        log.info("Enabled user: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "User not found");
        }

        user.setStatus(0);

        if (StpUtil.isLogin()) {
            user.setUpdateBy(StpUtil.getLoginIdAsLong());
        }

        userMapper.updateById(user);
        log.info("Disabled user: id={}", id);
    }
}
