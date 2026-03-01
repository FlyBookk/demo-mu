package com.musheng.business.advertising.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.advertising.entity.AdvertisingBill;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 广告发票主表Mapper
 */
@Mapper
public interface AdvertisingBillMapper extends BaseMapper<AdvertisingBill> {

    @Delete("DELETE FROM t_advertising_bill WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Delete("<script>" +
            "DELETE FROM t_advertising_bill WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") java.util.List<Long> ids);
}
