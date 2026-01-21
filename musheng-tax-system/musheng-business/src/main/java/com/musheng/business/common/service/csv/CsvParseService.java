package com.musheng.business.common.service.csv;

import org.springframework.web.multipart.MultipartFile;

/**
 * CSV解析服务接口
 * 用于解析亚马逊CSV文件的核心服务
 */
public interface CsvParseService {

    /**
     * 解析CSV文件表头
     * 检测表头行、站点编码和语言
     *
     * @param file 上传的CSV文件
     * @return 表头解析结果
     */
    CsvHeaderResult parseHeaders(MultipartFile file);

    /**
     * 检测站点/marketplace
     *
     * @param file 上传的CSV文件
     * @return 站点编码(US/CA/UK/DE)，无法检测则返回null
     */
    String detectMarketplace(MultipartFile file);

    /**
     * 解析销售数据
     *
     * @param file        上传的CSV文件
     * @param siteCode    站点编码(为null则自动检测)
     * @param entityClass 实体类类型
     * @param <T>         数据实体类型
     * @return 解析结果(包含数据列表)
     */
    <T> CsvParseResult<T> parseSalesData(MultipartFile file, String siteCode, Class<T> entityClass);

    /**
     * 解析配送数据
     *
     * @param file        上传的CSV文件
     * @param siteCode    站点编码(为null则自动检测)
     * @param entityClass 实体类类型
     * @param <T>         数据实体类型
     * @return 解析结果(包含数据列表)
     */
    <T> CsvParseResult<T> parseShippingData(MultipartFile file, String siteCode, Class<T> entityClass);

    /**
     * 解析汇率数据
     *
     * @param file        上传的CSV文件
     * @param entityClass 实体类类型
     * @param <T>         数据实体类型
     * @return 解析结果(包含数据列表)
     */
    <T> CsvParseResult<T> parseRateData(MultipartFile file, Class<T> entityClass);
}
