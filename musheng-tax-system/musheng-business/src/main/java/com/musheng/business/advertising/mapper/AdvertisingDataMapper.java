package com.musheng.business.advertising.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.advertising.entity.AdvertisingData;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Advertising Data Mapper
 */
@Mapper
public interface AdvertisingDataMapper extends BaseMapper<AdvertisingData> {

    /**
     * 物理删除广告数据（绕过逻辑删除）
     *
     * @param id 数据ID
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("DELETE FROM t_advertising_data WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
    
    /**
     * 批量物理删除广告数据（绕过逻辑删除）
     *
     * @param ids 数据ID列表
     * @return 影响行数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Delete("<script>" +
            "DELETE FROM t_advertising_data WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") java.util.List<Long> ids);
}
