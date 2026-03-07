-- 1. 销售数据中通过 shipping 关联的 income SKU 数量
SELECT COUNT(DISTINCT sd.sku) as sales_sku_count
FROM t_sales_data sd
INNER JOIN t_shipping_data sh ON sd.order_id = sh.order_id AND sh.shop_id = 1
WHERE sd.shop_id = 1 
  AND sd.transaction_category = 'income'
  AND sh.ship_date >= '2025-07-01' AND sh.ship_date <= '2025-09-30';
