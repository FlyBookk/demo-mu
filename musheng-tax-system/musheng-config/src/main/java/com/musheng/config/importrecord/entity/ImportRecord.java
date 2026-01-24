package com.musheng.config.importrecord.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.musheng.common.entity.BaseEntityMinimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 导入记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_import_record")
public class ImportRecord extends BaseEntityMinimal {

    /**
     * 店铺ID（业务数据导入关联店铺，汇率导入不关联）
     */
    private Long shopId;

    /**
     * 导入批次号
     */
    private String batchNo;

    /**
     * 数据类型(sales/shipping/advertising/rate)
     */
    private String dataType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功条数
     */
    private Integer successCount;

    /**
     * 失败条数
     */
    private Integer failCount;

    /**
     * 导入状态(pending/processing/success/partial/fail)
     */
    private String importStatus;

    /**
     * 错误信息摘要
     */
    private String errorMessage;

    /**
     * 导入人
     */
    private Long importBy;

    /**
     * 导入时间
     */
    private LocalDateTime importTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
