package com.musheng.config.dataclean.service.impl;

import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import com.musheng.config.dataclean.dto.DataCleanModuleVO;
import com.musheng.config.dataclean.service.DataCleanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 数据清理服务实现
 *
 * @author wanhua
 * 12:40 2026年03月08日
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataCleanServiceImpl implements DataCleanService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 模块定义：moduleCode -> (moduleName, description, 关联表列表)
     * 表的删除顺序：子表在前，主表在后
     */
    private static final LinkedHashMap<String, ModuleDef> MODULE_DEFS = new LinkedHashMap<>();

    static {
        MODULE_DEFS.put("sales", new ModuleDef(
                "销售数据", "清理销售结算数据",
                List.of("t_sales_data")));

        MODULE_DEFS.put("shipping", new ModuleDef(
                "配送数据", "清理配送费用数据",
                List.of("t_shipping_data")));

        MODULE_DEFS.put("advertising", new ModuleDef(
                "广告数据", "清理广告账单及明细",
                List.of("t_advertising_bill_item", "t_advertising_bill")));

        MODULE_DEFS.put("fba_shipment", new ModuleDef(
                "FBA货件", "清理FBA货件及SKU明细",
                List.of("t_fba_shipment_item", "t_fba_shipment")));

        MODULE_DEFS.put("document_po_dn", new ModuleDef(
                "PO/DN单据", "清理采购单和发货通知单",
                List.of("t_document_po_item", "t_document_po",
                        "t_document_dn_item", "t_document_dn")));

        MODULE_DEFS.put("settlement_import", new ModuleDef(
                "MSKU推算数据", "清理结算导入推算数据",
                List.of("t_settlement_import_data")));

        MODULE_DEFS.put("document_settlement_inv", new ModuleDef(
                "结算单/INV", "清理结算单和INV单据",
                List.of("t_document_settlement_item", "t_document_settlement",
                        "t_document_inv_item", "t_document_inv")));
    }

    @Override
    public List<DataCleanModuleVO> getModules() {
        Long shopId = ShopContext.requireShopId();
        List<DataCleanModuleVO> result = new ArrayList<>();

        for (Map.Entry<String, ModuleDef> entry : MODULE_DEFS.entrySet()) {
            ModuleDef def = entry.getValue();
            // 统计主表数据量（取列表中最后一个表，即主表）
            String mainTable = def.tables.get(def.tables.size() - 1);
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + mainTable + " WHERE shop_id = ?",
                    Long.class, shopId);

            result.add(DataCleanModuleVO.builder()
                    .moduleCode(entry.getKey())
                    .moduleName(def.name)
                    .description(def.description)
                    .dataCount(count != null ? count : 0L)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanModule(String moduleCode) {
        Long shopId = ShopContext.requireShopId();
        ModuleDef def = MODULE_DEFS.get(moduleCode);
        if (def == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的模块编码: " + moduleCode);
        }

        log.info("开始清理数据 - 店铺:{}, 模块:{}", shopId, def.name);
        int totalDeleted = 0;

        for (String table : def.tables) {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM " + table + " WHERE shop_id = ?", shopId);
            log.info("清理表 {} - 删除 {} 条", table, deleted);
            totalDeleted += deleted;
        }

        log.info("数据清理完成 - 店铺:{}, 模块:{}, 共删除 {} 条", shopId, def.name, totalDeleted);
        return totalDeleted;
    }

    /**
     * 模块定义内部类
     */
    private record ModuleDef(String name, String description, List<String> tables) {
    }
}
