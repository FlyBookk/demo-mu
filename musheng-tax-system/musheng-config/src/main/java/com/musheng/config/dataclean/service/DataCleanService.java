package com.musheng.config.dataclean.service;

import com.musheng.config.dataclean.dto.DataCleanModuleVO;

import java.util.List;

/**
 * 数据清理服务接口
 *
 * @author wanhua
 * 12:40 2026年03月08日
 */
public interface DataCleanService {

    /**
     * 获取可清理的模块列表（含当前店铺数据量）
     *
     * @return 模块列表
     */
    List<DataCleanModuleVO> getModules();

    /**
     * 按模块清理当前店铺数据
     *
     * @param moduleCode 模块编码
     * @param siteCode   站点编码（可选，为空则清理所有站点）
     * @return 清理的数据条数
     */
    int cleanModule(String moduleCode, String siteCode);
}
