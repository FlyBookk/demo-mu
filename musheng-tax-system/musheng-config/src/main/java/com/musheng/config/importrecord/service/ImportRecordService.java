package com.musheng.config.importrecord.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.config.importrecord.dto.ImportRecordQueryRequest;
import com.musheng.config.importrecord.entity.ImportRecord;

/**
 * 导入记录服务接口
 */
public interface ImportRecordService {

    /**
     * 根据ID获取导入记录
     *
     * @param id 实体ID
     * @return 实体
     */
    ImportRecord getById(Long id);

    /**
     * 删除导入记录
     *
     * @param id 实体ID
     */
    void delete(Long id);

    /**
     * 分页查询导入记录
     *
     * @param queryRequest 查询参数
     * @return 分页结果
     */
    Page<ImportRecord> list(ImportRecordQueryRequest queryRequest);
}
