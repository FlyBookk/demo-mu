package com.musheng.config.importrecord.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.importrecord.dto.ImportRecordQueryRequest;
import com.musheng.config.importrecord.entity.ImportRecord;
import com.musheng.config.importrecord.mapper.ImportRecordMapper;
import com.musheng.config.importrecord.service.ImportRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Import Record Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportRecordServiceImpl implements ImportRecordService {

    private final ImportRecordMapper importRecordMapper;

    @Override
    public ImportRecord getById(Long id) {
        ImportRecord entity = importRecordMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Import record not found");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ImportRecord entity = importRecordMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST, "Import record not found");
        }

        importRecordMapper.deleteById(id);
        log.info("Deleted import record: id={}, batchNo={}", id, entity.getBatchNo());
    }

    @Override
    public Page<ImportRecord> list(ImportRecordQueryRequest queryRequest) {
        LambdaQueryWrapper<ImportRecord> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(queryRequest.getBatchNo())) {
            wrapper.like(ImportRecord::getBatchNo, queryRequest.getBatchNo());
        }
        if (StringUtils.hasText(queryRequest.getDataType())) {
            wrapper.eq(ImportRecord::getDataType, queryRequest.getDataType());
        }
        if (StringUtils.hasText(queryRequest.getFileName())) {
            wrapper.like(ImportRecord::getFileName, queryRequest.getFileName());
        }
        if (StringUtils.hasText(queryRequest.getImportStatus())) {
            wrapper.eq(ImportRecord::getImportStatus, queryRequest.getImportStatus());
        }

        wrapper.orderByDesc(ImportRecord::getImportTime);

        return importRecordMapper.selectPage(
                new Page<>(queryRequest.getPage(), queryRequest.getSize()),
                wrapper
        );
    }
}
