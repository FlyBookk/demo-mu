package com.musheng.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.system.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置 Mapper
 *
 * @author wanhua
 * 21:35 2026年03月21日
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据配置键查询配置值
     *
     * @param configKey 配置键
     * @return 配置值
     * @author wanhua
     * 21:35 2026年03月21日
     */
    @Select("SELECT config_value FROM t_sys_config WHERE config_key = #{configKey}")
    String selectValueByKey(@Param("configKey") String configKey);
}
