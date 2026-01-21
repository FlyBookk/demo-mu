package com.musheng.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志实体
 * 映射到 t_log_operation 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_log_operation")
public class OperationLog extends BaseEntity {

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作类型
     */
    @TableField("operation")
    private String operation;

    /**
     * 请求方法(GET, POST, PUT, DELETE)
     */
    @TableField("method")
    private String method;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数(JSON)
     */
    private String requestParams;

    /**
     * 响应数据(JSON)
     */
    @TableField("response_data")
    private String responseData;

    /**
     * 操作IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 执行时长(毫秒)
     */
    private Long executionTime;

    /**
     * 状态(1成功, 0失败)
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;
}
