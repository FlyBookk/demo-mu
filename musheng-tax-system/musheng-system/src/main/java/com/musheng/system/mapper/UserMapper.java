package com.musheng.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * User Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * Select user by username
     */
    @Select("SELECT * FROM t_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);
}
