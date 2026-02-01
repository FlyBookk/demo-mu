package com.musheng.business.fbashipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.fbashipment.entity.FbaShipmentItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * FBA货件明细Mapper接口
 * 提供SKU明细级别的数据访问操作
 */
@Mapper
public interface FbaShipmentItemMapper extends BaseMapper<FbaShipmentItem> {
    // MyBatis-Plus提供了基础的CRUD方法
    // 如需自定义查询，可在此添加方法
}
