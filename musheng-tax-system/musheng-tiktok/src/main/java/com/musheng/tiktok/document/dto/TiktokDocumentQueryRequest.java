package com.musheng.tiktok.document.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * TK单据列表查询请求
 *
 * @author wanhua
 * 21:12 2026年05月14日
 */
@Data
@Builder
public class TiktokDocumentQueryRequest {
    private String documentType;
    private String documentNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private String siteCode;
    @Builder.Default
    private Integer pageNum = 1;
    @Builder.Default
    private Integer pageSize = 10;
}
