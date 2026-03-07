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
     * 按周期起止日期和站点代码查询未删除的结算数据
     *
     * @param periodStart 周期起始日期
     * @param periodEnd 周期结束日期
     * @param siteCode 站点代码
     * @return 符合条件的结算导入数据列表
     * @author wanhua
     * 10:30 2026年01月29日
     */
    @Select("SELECT * FROM t_settlement_import_data " +
            "WHERE period_start = #{periodStart} " +
            "AND period_end = #{periodEnd} " +
            "AND site_code = #{siteCode} " +
            "AND del_flag = 0")
    List<SettlementImportData> selectByPeriodAndSite(
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
            "WHERE period_start = #{periodStart} " +
            "AND period_end = #{periodEnd} " +
            "AND site_code = #{siteCode} " +
            "AND del_flag = 0")
    int logicalDeleteByPeriodAndSite(
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("siteCode") String siteCode);
}
