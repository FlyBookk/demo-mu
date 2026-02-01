package com.musheng.business.fbashipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.fbashipment.entity.FbaShipment;
import org.apache.ibatis.annotations.Mapper;

/**
 * FBA货件主表Mapper接口
 * 提供货件级别的数据访问操作
 */
@Mapper
public interface FbaShipmentMapper extends BaseMapper<FbaShipment> {
    // MyBatis-Plus提供了基础的CRUD方法
    // 如需自定义查询，可在此添加方法
}
