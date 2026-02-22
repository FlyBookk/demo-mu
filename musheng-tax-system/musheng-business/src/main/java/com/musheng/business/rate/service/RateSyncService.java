package com.musheng.business.rate.service;

import com.musheng.business.rate.dto.RateSyncResultDTO;

/**
 * 汇率同步服务接口
 * 仅支持通过 curl 命令同步（用户从中国货币网复制 curl）
 */
public interface RateSyncService {

    /**
     * 通过粘贴的 curl 命令同步汇率
     * 用户从中国货币网页面 F12 → Network → CcprHisNew → 右键 Copy as cURL
     *
     * @param curlCommand 完整的 curl 命令
     * @return 同步结果
     */
    RateSyncResultDTO syncFromCurl(String curlCommand);
}
