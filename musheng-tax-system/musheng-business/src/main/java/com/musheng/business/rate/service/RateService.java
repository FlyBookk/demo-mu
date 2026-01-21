package com.musheng.business.rate.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.business.rate.dto.RateConvertRequest;
import com.musheng.business.rate.dto.RateConvertResultDTO;
import com.musheng.business.rate.dto.RateRequest;
import com.musheng.business.rate.entity.ExchangeRate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 汇率服务接口
 */
public interface RateService {

    /**
     * 分页查询汇率
     *
     * @param currencyCode 货币编码(可选)
     * @param startDate    开始日期(可选)
     * @param endDate      结束日期(可选)
     * @param source       数据来源(可选): PBOC/MANUAL/IMPORT
     * @param page         页码
     * @param size         每页条数
     * @return 分页结果
     */
    Page<ExchangeRate> list(String currencyCode, LocalDate startDate, LocalDate endDate, String source, int page, int size);

    /**
     * 根据ID获取汇率详情
     *
     * @param id 汇率ID
     * @return 汇率详情
     */
    ExchangeRate getById(Long id);

    /**
     * 新增汇率（手动录入）
     *
     * @param request 汇率请求
     * @return 新增的汇率
     */
    ExchangeRate create(RateRequest request);

    /**
     * 更新汇率
     *
     * @param id      汇率ID
     * @param request 汇率请求
     * @return 更新后的汇率
     */
    ExchangeRate update(Long id, RateRequest request);

    /**
     * 删除汇率
     *
     * @param id 汇率ID
     */
    void delete(Long id);

    /**
     * 批量删除汇率
     *
     * @param ids 汇率ID列表
     */
    void batchDelete(java.util.List<Long> ids);

    /**
     * 根据货币编码和日期获取汇率
     *
     * @param currencyCode 货币编码
     * @param date         日期(YYYY-MM-DD)
     * @return 汇率值(对人民币)
     */
    BigDecimal getRate(String currencyCode, String date);

    /**
     * 从文件导入汇率
     *
     * @param file 导入文件
     * @return 导入结果(包含成功/失败数量)
     */
    Map<String, Object> importData(MultipartFile file);

    /**
     * 货币转换
     *
     * @param request 转换请求(金额、货币编码、汇率日期)
     * @return 转换结果(包含转换后金额、使用的汇率等信息)
     */
    RateConvertResultDTO convertCurrency(RateConvertRequest request);
}
