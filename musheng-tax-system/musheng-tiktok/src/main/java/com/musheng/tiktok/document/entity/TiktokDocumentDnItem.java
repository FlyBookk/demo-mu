package com.musheng.tiktok.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_tiktok_document_dn_item")
public class TiktokDocumentDnItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long shopId;
    private String siteCode;
    private Long dnId;
    private Integer lineNo;
    private String msku;
    private Integer quantity;
    private String shipmentNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
