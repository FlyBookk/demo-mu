package com.musheng.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ============== 系统级错误 (1xxxx) ==============
    SYSTEM_ERROR(10001, "系统异常"),
    PARAM_ERROR(10002, "参数错误"),
    PARAM_MISSING(10003, "必填参数缺失"),
    DATA_NOT_EXIST(10004, "数据不存在"),
    DATA_ALREADY_EXIST(10005, "数据已存在"),
    OPERATION_FAILED(10006, "操作失败"),

    // ============== 认证授权错误 (2xxxx) ==============
    UNAUTHORIZED(20001, "未登录或登录已过期"),
    FORBIDDEN(20002, "权限不足"),
    LOGIN_FAILED(20003, "用户名或密码错误"),
    ACCOUNT_LOCKED(20004, "账号已被锁定"),
    ACCOUNT_DISABLED(20005, "账号已被禁用"),
    TOKEN_INVALID(20006, "Token无效"),
    TOKEN_EXPIRED(20007, "Token已过期"),
    PASSWORD_ERROR(20008, "密码错误"),
    SHOP_NOT_SELECTED(20009, "请先选择店铺"),

    // ============== 数据导入错误 (3xxxx) ==============
    IMPORT_FILE_EMPTY(30001, "导入文件为空"),
    IMPORT_FILE_FORMAT_ERROR(30002, "文件格式错误"),
    IMPORT_HEADER_NOT_FOUND(30003, "无法识别表头"),
    IMPORT_SITE_NOT_MATCH(30004, "站点无法匹配"),
    IMPORT_DUPLICATE_DATA(30005, "数据重复"),
    IMPORT_FIELD_REQUIRED(30006, "必填字段缺失"),
    IMPORT_PARSE_ERROR(30007, "文件解析错误"),
    IMPORT_DATE_FORMAT_ERROR(30008, "日期格式错误"),
    IMPORT_NUMBER_FORMAT_ERROR(30009, "数字格式错误"),
    IMPORT_DUPLICATE(30010, "文件已导入，请勿重复导入"),

    // ============== 汇率相关错误 (31xxx) ==============
    RATE_NOT_FOUND(31001, "汇率数据不存在"),
    RATE_DATE_INVALID(31002, "汇率日期无效"),
    RATE_CURRENCY_NOT_SUPPORT(31003, "货币类型不支持"),
    EXTERNAL_API_ERROR(31004, "外部接口调用失败"),

    // ============== 汇总报表错误 (32xxx) ==============
    SUMMARY_DATA_INCOMPLETE(32001, "汇总数据不完整"),
    SUMMARY_RATE_MISSING(32002, "缺少汇率数据"),
    SUMMARY_CALCULATE_ERROR(32003, "汇总计算错误"),

    // ============== 文件上传错误 (33xxx) ==============
    UPLOAD_FILE_EMPTY(33001, "上传文件为空"),
    UPLOAD_FILE_SIZE_EXCEED(33002, "文件大小超过限制"),
    UPLOAD_FILE_TYPE_ERROR(33003, "文件类型不允许"),
    UPLOAD_CHUNK_ERROR(33004, "分片上传失败"),
    UPLOAD_MERGE_ERROR(33005, "分片合并失败"),

    // ============== 数据导出错误 (34xxx) ==============
    EXPORT_FAILED(34001, "导出失败"),
    
    // ============== 文件处理错误 (35xxx) ==============
    FILE_UPLOAD_ERROR(35001, "文件上传失败"),
    FILE_NOT_FOUND(35002, "文件不存在或已过期"),
    FILE_READ_ERROR(35003, "文件读取失败"),
    
    // ============== 导入执行错误 (36xxx) ==============
    IMPORT_FAILED(36001, "导入执行失败"),
    IMPORT_PARTIAL_FAILED(36002, "部分数据导入失败");

    /**
     * 错误代码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;
}
