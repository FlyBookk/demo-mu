package com.musheng.config;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.musheng.system.entity.Role;
import com.musheng.system.entity.User;
import com.musheng.system.mapper.RoleMapper;
import com.musheng.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token权限实现类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    /**
     * 获取用户权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }

        Role role = roleMapper.selectByCode(user.getRoleCode());
        if (role == null || role.getPermissions() == null) {
            return Collections.emptyList();
        }

        try {
            JSONArray array = JSONUtil.parseArray(role.getPermissions());
            return array.toList(String.class);
        } catch (Exception e) {
            log.warn("解析用户权限失败: {}", user.getUsername());
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(user.getRoleCode());
    }
}
