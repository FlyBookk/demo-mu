package com.musheng.business.rate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通过 curl 同步汇率的请求
 */
@Data
public class CurlSyncRequest {

    /**
     * 完整的 curl 命令（从浏览器 Copy as cURL 复制）
     */
    @NotBlank(message = "curl 命令不能为空")
    private String curl;
}
