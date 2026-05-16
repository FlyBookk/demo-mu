package com.musheng.tiktok.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_tiktok_document_po_item")
public class TiktokDocumentPoItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String siteCode;
    private Long poId;
    private String shipmentNo;
    private String msku;
    private Integer quantity;
    private String fbtAddress;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
