package com.musheng.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体
 *
 * @author wanhua
 * 21:35 2026年03月21日
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sys_config")
public class SysConfig extends BaseEntity {

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置描述 */
    private String configDesc;
}
