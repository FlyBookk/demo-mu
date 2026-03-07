package com.musheng.business.settlement.derivation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推导确认写入结果 VO
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推导确认写入结果")
public class DerivationConfirmResultVO {

    /** 推导批次标识（UUID） */
    @Schema(description = "推导批次标识（UUID）")
    private String settlementBatchId;

    /** 写入记录条数 */
    @Schema(description = "写入记录条数")
    private int recordCount;

    /** 覆盖时逻辑删除的旧记录数 */
    @Schema(description = "覆盖时逻辑删除的旧记录数")
    private int deletedCount;
}
