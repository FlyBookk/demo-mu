package com.musheng.common.constant;

/**
 * 通用常量类
 */
public class CommonConstants {

    private CommonConstants() {
    }

    /**
     * 状态 - 启用
     */
    public static final int STATUS_ENABLED = 1;

    /**
     * 状态 - 禁用
     */
    public static final int STATUS_DISABLED = 0;

    /**
     * 删除标记 - 正常
     */
    public static final int DELETED_NO = 0;

    /**
     * 删除标记 - 已删除
     */
    public static final int DELETED_YES = 1;

    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE = 1;

    /**
     * 默认每页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页条数
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * 批量插入条数
     */
    public static final int BATCH_SIZE = 1000;

    /**
     * 日期格式
     */
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_PATTERN = "HH:mm:ss";

    /**
     * 导入状态
     */
    public static final String IMPORT_STATUS_PENDING = "pending";
    public static final String IMPORT_STATUS_PROCESSING = "processing";
    public static final String IMPORT_STATUS_SUCCESS = "success";
    public static final String IMPORT_STATUS_PARTIAL = "partial";
    public static final String IMPORT_STATUS_FAIL = "fail";

    /**
     * 缓存状态
     */
    public static final String CACHE_STATUS_VALID = "valid";
    public static final String CACHE_STATUS_INVALID = "invalid";
    public static final String CACHE_STATUS_CALCULATING = "calculating";

    /**
     * 汇率顺延最大天数
     */
    public static final int RATE_DEFER_MAX_DAYS = 10;

    /**
     * 采购金额计算比例
     */
    public static final String PURCHASE_RATIO = "0.96";
}
