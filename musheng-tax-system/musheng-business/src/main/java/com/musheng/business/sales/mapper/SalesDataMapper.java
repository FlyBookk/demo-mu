package com.musheng.business.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.sales.entity.SalesData;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Sales Data Mapper
 */
@Mapper
public interface SalesDataMapper extends BaseMapper<SalesData> {

    /**
     * 物理删除销售数据（绕过逻辑删除）
     *
     * @param id 数据ID
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("DELETE FROM t_sales_data WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
    
    /**
     * 批量物理删除销售数据（绕过逻辑删除）
     *
     * @param ids 数据ID列表
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("<script>" +
            "DELETE FROM t_sales_data WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") java.util.List<Long> ids);
}
