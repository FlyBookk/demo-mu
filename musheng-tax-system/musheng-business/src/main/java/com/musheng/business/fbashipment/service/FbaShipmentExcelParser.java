package com.musheng.business.fbashipment.service;

import com.musheng.business.fbashipment.entity.FbaShipment;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * FBA货件Excel解析服务接口
 * 负责解析Excel文件并转换为货件和明细对象
 */
public interface FbaShipmentExcelParser {

    /**
     * 解析Excel文件
     *
     * @param file Excel文件
     * @param shopId 店铺ID
     * @param importBatchId 导入批次ID
     * @return 解析后的货件列表（包含明细）
     * @throws Exception 解析异常
     */
    List<FbaShipment> parseExcel(MultipartFile file, Long shopId, Long importBatchId) throws Exception;
}
