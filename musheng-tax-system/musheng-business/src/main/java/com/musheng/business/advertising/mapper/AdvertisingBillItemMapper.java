package com.musheng.business.advertising.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.advertising.entity.AdvertisingBillItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 广告发票明细表Mapper
 */
@Mapper
public interface AdvertisingBillItemMapper extends BaseMapper<AdvertisingBillItem> {

    @Delete("DELETE FROM t_advertising_bill_item WHERE bill_id = #{billId}")
    int deleteByBillId(@Param("billId") Long billId);

    @Delete("<script>" +
            "DELETE FROM t_advertising_bill_item WHERE bill_id IN " +
            "<foreach collection='billIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int deleteByBillIds(@Param("billIds") java.util.List<Long> billIds);
}
