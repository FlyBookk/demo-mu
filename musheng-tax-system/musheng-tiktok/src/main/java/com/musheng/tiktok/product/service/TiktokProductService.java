package com.musheng.tiktok.product.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.musheng.common.context.ShopContext;
import com.musheng.tiktok.product.entity.TiktokProduct;
import com.musheng.tiktok.product.mapper.TiktokProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TK商品库服务
 *
 * @author wanhua
 * 19:18 2026年05月14日
 */
@Service
@Slf4j
public class TiktokProductService {

    @Autowired
    private TiktokProductMapper productMapper;

    /**
     * 分页查询商品列表
     */
    public Page<TiktokProduct> list(String keyword, String siteCode, Integer current, Integer size) {
        Long shopId = ShopContext.requireShopId();
        Page<TiktokProduct> page = new Page<>(current, size);
        LambdaQueryWrapper<TiktokProduct> wrapper = new LambdaQueryWrapper<TiktokProduct>()
                .eq(TiktokProduct::getShopId, shopId)
                .eq(TiktokProduct::getSiteCode, siteCode)
                .orderByAsc(TiktokProduct::getMsku);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(TiktokProduct::getMsku, keyword)
                    .or().like(TiktokProduct::getProductName, keyword)
                    .or().like(TiktokProduct::getSkuId, keyword));
        }
        wrapper.orderByDesc(TiktokProduct::getUpdateTime);
        return productMapper.selectPage(page, wrapper);
    }

    /**
     * 根据 skuId 列表批量查询（结算单导入校验用）
     */
    public Map<String, TiktokProduct> findBySkuIds(Long shopId, Collection<String> skuIds) {
        if (CollectionUtils.isEmpty(skuIds)) {
            return Collections.emptyMap();
        }
        List<TiktokProduct> products = productMapper.selectList(
                new LambdaQueryWrapper<TiktokProduct>()
                        .eq(TiktokProduct::getShopId, shopId)
                        .in(TiktokProduct::getSkuId, skuIds));
        return products.stream().collect(Collectors.toMap(TiktokProduct::getSkuId, p -> p, (a, b) -> a));
    }

    /**
     * 导入SKU对照表
     *
     * @return 导入结果：新增数/更新数
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importExcel(MultipartFile file, String siteCode) throws IOException {
        Long shopId = ShopContext.requireShopId();
        int[] counts = {0, 0}; // [新增, 更新]

        // 先读取表头行，建立 title → colIndex 映射
        final Map<String, Integer> colMap = new java.util.HashMap<>();
        final boolean[] headerParsed = {false};

        EasyExcel.read(file.getInputStream())
                .sheet(0)
                .headRowNumber(0)
                .registerReadListener(new PageReadListener<Map<Integer, String>>(rows -> {
                    for (Map<Integer, String> row : rows) {
                        // 解析表头行（第一行包含 product_id）
                        if (!headerParsed[0]) {
                            String first = row.get(0);
                            if ("product_id".equals(first)) {
                                for (Map.Entry<Integer, String> e : row.entrySet()) {
                                    if (StringUtils.hasText(e.getValue())) {
                                        colMap.put(e.getValue().trim().toLowerCase(), e.getKey());
                                    }
                                }
                                headerParsed[0] = true;
                            }
                            continue;
                        }

                        // 跳过说明行
                        String productId = row.get(colMap.getOrDefault("product_id", 0));
                        if (!StringUtils.hasText(productId) || productId.startsWith("V")
                                || "商品 ID".equals(productId) || "必填".equals(productId)
                                || "不可编辑".equals(productId)) {
                            continue;
                        }

                        int colSkuId = colMap.getOrDefault("sku_id", 3);
                        int colMsku = colMap.getOrDefault("seller_sku", 8);
                        int colName = colMap.getOrDefault("product_name", 2);
                        int colCategory = colMap.getOrDefault("category", 1);
                        int colVariation = colMap.getOrDefault("variation_value", 4);
                        int colPrice = colMap.getOrDefault("price", 5);

                        String skuId = row.get(colSkuId);
                        String msku = row.get(colMsku);
                        if (!StringUtils.hasText(skuId) || !StringUtils.hasText(msku)) {
                            continue;
                        }

                        // 查询是否已存在
                        TiktokProduct existing = productMapper.selectOne(
                                new LambdaQueryWrapper<TiktokProduct>()
                                        .eq(TiktokProduct::getShopId, shopId)
                                        .eq(TiktokProduct::getSiteCode, siteCode)
                                        .eq(TiktokProduct::getSkuId, skuId));

                        if (existing != null) {
                            existing.setMsku(msku);
                            existing.setProductId(productId);
                            existing.setProductName(row.getOrDefault(colName, ""));
                            existing.setCategory(row.getOrDefault(colCategory, ""));
                            existing.setVariationValue(row.getOrDefault(colVariation, ""));
                            String priceStr = row.get(colPrice);
                            if (StringUtils.hasText(priceStr)) {
                                try { existing.setPrice(new BigDecimal(priceStr)); } catch (Exception ignored) {}
                            }
                            productMapper.updateById(existing);
                            counts[1]++;
                        } else {
                            TiktokProduct product = new TiktokProduct();
                            product.setShopId(shopId);
                            product.setSiteCode(siteCode);
                            product.setProductId(productId);
                            product.setSkuId(skuId);
                            product.setMsku(msku);
                            product.setProductName(row.getOrDefault(colName, ""));
                            product.setCategory(row.getOrDefault(colCategory, ""));
                            product.setVariationValue(row.getOrDefault(colVariation, ""));
                            String priceStr = row.get(colPrice);
                            if (StringUtils.hasText(priceStr)) {
                                try { product.setPrice(new BigDecimal(priceStr)); } catch (Exception ignored) {}
                            }
                            product.setStatus(1);
                            productMapper.insert(product);
                            counts[0]++;
                        }
                    }
                }, 100))
                .doRead();

        log.info("TK商品库导入完成: shopId={}, 新增={}, 更新={}", shopId, counts[0], counts[1]);
        return Map.of("inserted", counts[0], "updated", counts[1]);
    }

    /**
     * 更新商品MSKU
     */
    public void updateMsku(Long id, String msku) {
        Long shopId = ShopContext.requireShopId();
        TiktokProduct product = productMapper.selectById(id);
        if (product == null || !shopId.equals(product.getShopId())) {
            throw new RuntimeException("商品不存在或无权操作");
        }
        product.setMsku(msku);
        productMapper.updateById(product);
    }
}
