package com.musheng.tiktok.shipment.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.shipment.entity.TiktokShipment;
import com.musheng.tiktok.shipment.entity.TiktokShipmentItem;
import com.musheng.tiktok.shipment.mapper.TiktokShipmentItemMapper;
import com.musheng.tiktok.shipment.mapper.TiktokShipmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * TK FBT货件服务
 *
 * @author wanhua
 * 19:38 2026年05月14日
 */
@Service
@Slf4j
public class TiktokShipmentService {

    @Autowired
    private TiktokShipmentMapper shipmentMapper;
    @Autowired
    private TiktokShipmentItemMapper itemMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 货件列表（分页）
     */
    public Page<TiktokShipment> list(String keyword, String siteCode, java.time.LocalDate startDate, java.time.LocalDate endDate, Integer current, Integer size) {
        Long shopId = ShopContext.requireShopId();
        LambdaQueryWrapper<TiktokShipment> wrapper = new LambdaQueryWrapper<TiktokShipment>()
                .eq(TiktokShipment::getShopId, shopId)
                .eq(TiktokShipment::getSiteCode, siteCode);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(TiktokShipment::getShipmentId, keyword)
                    .or().like(TiktokShipment::getWarehouseCode, keyword));
        }
        if (startDate != null) {
            wrapper.ge(TiktokShipment::getCreationTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.le(TiktokShipment::getCreationTime, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(TiktokShipment::getCreationTime);
        return shipmentMapper.selectPage(new Page<>(current, size), wrapper);
    }

    /**
     * 货件明细
     */
    public List<TiktokShipmentItem> getItems(String shipmentId, String siteCode) {
        Long shopId = ShopContext.requireShopId();
        return itemMapper.selectList(new LambdaQueryWrapper<TiktokShipmentItem>()
                .eq(TiktokShipmentItem::getShopId, shopId)
                .eq(TiktokShipmentItem::getSiteCode, siteCode)
                .eq(TiktokShipmentItem::getShipmentId, shipmentId));
    }

    /**
     * 导入整合版Excel（货件详情sheet）
     * 格式：货件单号 | 货件名称 | 状态 | RefID | 创建时间 | 更新时间 | MSKU | 申报量 | 签收量 | 收件人 | 物流中心 | 邮编 | 国家 | 州 | 城市 | 街道 | 门牌号
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importExcel(MultipartFile file, String siteCode) throws IOException {
        Long shopId = ShopContext.requireShopId();
        int[] counts = {0, 0}; // [货件数, 明细数]

        EasyExcel.read(file.getInputStream())
                .sheet("货件详情")
                .headRowNumber(1)
                .registerReadListener(new PageReadListener<Map<Integer, String>>(rows -> {
                    String currentShipmentId = null;

                    for (Map<Integer, String> row : rows) {
                        String shipmentIdVal = row.get(0);
                        String msku = row.get(6);

                        // 新货件行（货件单号非空）
                        if (StringUtils.hasText(shipmentIdVal)) {
                            currentShipmentId = shipmentIdVal;
                            // 检查是否已存在
                            Long existCount = shipmentMapper.selectCount(
                                    new LambdaQueryWrapper<TiktokShipment>()
                                            .eq(TiktokShipment::getShopId, shopId)
                                            .eq(TiktokShipment::getSiteCode, siteCode)
                                            .eq(TiktokShipment::getShipmentId, shipmentIdVal));
                            if (existCount == 0) {
                                TiktokShipment shipment = new TiktokShipment();
                                shipment.setShopId(shopId);
                                shipment.setSiteCode(siteCode);
                                shipment.setShipmentId(shipmentIdVal);
                                shipment.setShipmentName(row.getOrDefault(1, ""));
                                shipment.setStatus(row.getOrDefault(2, ""));
                                shipment.setReferenceId(row.getOrDefault(3, ""));
                                shipment.setCreationTime(parseDateTime(row.get(4)));
                                shipment.setUpdateTimeSource(parseDateTime(row.get(5)));
                                shipment.setShipTo(row.getOrDefault(9, ""));
                                shipment.setWarehouseCode(row.getOrDefault(10, ""));
                                shipment.setPostalCode(row.getOrDefault(11, ""));
                                shipment.setCountry(row.getOrDefault(12, ""));
                                shipment.setState(row.getOrDefault(13, ""));
                                shipment.setCity(row.getOrDefault(14, ""));
                                String street = row.getOrDefault(15, "");
                                String doorNo = row.getOrDefault(16, "");
                                shipment.setStreetAddress(StringUtils.hasText(doorNo) ? street + " " + doorNo : street);
                                shipment.setTotalSkus(0);
                                shipment.setTotalQuantity(0);
                                shipmentMapper.insert(shipment);
                                counts[0]++;
                            }
                        }

                        // 明细行（MSKU非空）
                        if (StringUtils.hasText(msku) && StringUtils.hasText(currentShipmentId)) {
                            TiktokShipmentItem item = new TiktokShipmentItem();
                            item.setShopId(shopId);
                            item.setSiteCode(siteCode);
                            item.setShipmentId(currentShipmentId);
                            item.setMsku(msku);
                            item.setQuantityDeclared(parseIntSafe(row.get(7)));
                            item.setQuantityReceived(parseIntSafe(row.get(8)));
                            itemMapper.insert(item);
                            counts[1]++;
                        }
                    }
                }, 200))
                .doRead();

        // 更新货件的SKU数和总数量
        updateShipmentTotals(shopId);

        log.info("TK FBT货件导入完成: shopId={}, 货件={}, 明细={}", shopId, counts[0], counts[1]);
        return Map.of("shipments", counts[0], "items", counts[1]);
    }

    private void updateShipmentTotals(Long shopId) {
        List<TiktokShipment> shipments = shipmentMapper.selectList(
                new LambdaQueryWrapper<TiktokShipment>().eq(TiktokShipment::getShopId, shopId));
        for (TiktokShipment s : shipments) {
            List<TiktokShipmentItem> items = itemMapper.selectList(
                    new LambdaQueryWrapper<TiktokShipmentItem>()
                            .eq(TiktokShipmentItem::getShipmentId, s.getShipmentId())
                            .eq(TiktokShipmentItem::getShopId, shopId));
            s.setTotalSkus(items.size());
            s.setTotalQuantity(items.stream().mapToInt(i -> i.getQuantityReceived() != null ? i.getQuantityReceived() : 0).sum());
            shipmentMapper.updateById(s);
        }
    }

    private LocalDateTime parseDateTime(String val) {
        if (!StringUtils.hasText(val)) return null;
        try { return LocalDateTime.parse(val, DT_FMT); } catch (Exception e) { return null; }
    }

    private int parseIntSafe(String val) {
        if (!StringUtils.hasText(val)) return 0;
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return 0; }
    }
}
