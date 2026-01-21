package com.musheng.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Operation Log Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
