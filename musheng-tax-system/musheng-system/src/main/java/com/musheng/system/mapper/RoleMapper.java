package com.musheng.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Role Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * Select role by code
     */
    @Select("SELECT * FROM t_role WHERE role_code = #{roleCode} AND status = 1")
    Role selectByCode(@Param("roleCode") String roleCode);
}
