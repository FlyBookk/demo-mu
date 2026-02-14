package com.musheng.business.fbashipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.fbashipment.entity.FbaShipment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FBA货件主表Mapper接口
 * 提供货件级别的数据访问操作
 */
@Mapper
public interface FbaShipmentMapper extends BaseMapper<FbaShipment> {
    // MyBatis-Plus提供了基础的CRUD方法
    // 如需自定义查询，可在此添加方法

    /**
     * 物理删除FBA货件数据（绕过逻辑删除）
     *
     * @param id 数据ID
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("DELETE FROM t_fba_shipment WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
    
    /**
     * 批量物理删除FBA货件数据（绕过逻辑删除）
     *
     * @param ids 数据ID列表
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("<script>" +
            "DELETE FROM t_fba_shipment WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") java.util.List<Long> ids);
}
