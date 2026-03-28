package com.musheng.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.musheng.business.document.entity.SettlementImportData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 结算导入数据 Mapper 接口
 *
 * @author wanhua
 * 10:30 2026年01月29日
 */
@Mapper
public interface SettlementImportDataMapper extends BaseMapper<SettlementImportData> {

    /**
     * 按店铺、周期起止日期和站点代码查询未删除的结算数据
     *
     * @param shopId      店铺ID
     * @param periodStart 周期起始日期
     * @param periodEnd   周期结束日期
     * @param siteCode    站点代码
     * @return 符合条件的结算导入数据列表
     */
    @Select("SELECT * FROM t_settlement_import_data " +
            "WHERE shop_id = #{shopId} " +
            "AND period_start = #{periodStart} " +
            "AND period_end = #{periodEnd} " +
            "AND site_code = #{siteCode} " +
            "AND del_flag = 0")
    List<SettlementImportData> selectByPeriodAndSite(
            @Param("shopId") Long shopId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("siteCode") String siteCode);

    /**
     * 按周期起止日期和站点代码批量逻辑删除结算数据（将 del_flag 更新为 1）
     * <p>
     * 使用 @Update 注解直接写 SQL，绕过 @TableLogic 的自动过滤，
     * 确保能正确更新 del_flag=0 的记录为 del_flag=1
     * </p>
     *
     * @param periodStart 周期起始日期
     * @param periodEnd 周期结束日期
     * @param siteCode 站点代码
     * @return 逻辑删除的记录数
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Update("UPDATE t_settlement_import_data " +
            "SET del_flag = 1 " +
            "WHERE shop_id = #{shopId} " +
            "AND period_start = #{periodStart} " +
            "AND period_end = #{periodEnd} " +
            "AND site_code = #{siteCode} " +
            "AND del_flag = 0")
    int logicalDeleteByPeriodAndSite(
            @Param("shopId") Long shopId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("siteCode") String siteCode);

    /**
     * 按季度范围、店铺和站点批量逻辑删除（含该季度内所有月份分拆的记录）
     *
     * <p>匹配条件：shop_id = #{shopId} AND period_start >= quarterStart AND period_start <= quarterEnd，
     * 用于推导重跑时清除同一季度下已存储的所有月份记录，严格隔离店铺数据。</p>
     *
     * @param shopId       店铺ID（硬性条件，不可省略）
     * @param quarterStart 季度起始日期
     * @param quarterEnd   季度结束日期
     * @param siteCode     站点代码
     * @return 逻辑删除的记录数
     */
    @Update("UPDATE t_settlement_import_data " +
            "SET del_flag = 1 " +
            "WHERE shop_id = #{shopId} " +
            "AND period_start >= #{quarterStart} " +
            "AND period_start <= #{quarterEnd} " +
            "AND site_code = #{siteCode} " +
            "AND del_flag = 0")
    int logicalDeleteByPeriodRangeAndSite(
            @Param("shopId") Long shopId,
            @Param("quarterStart") LocalDate quarterStart,
            @Param("quarterEnd") LocalDate quarterEnd,
            @Param("siteCode") String siteCode);
}
