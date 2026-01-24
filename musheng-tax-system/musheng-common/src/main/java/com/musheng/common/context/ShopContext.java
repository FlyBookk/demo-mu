package com.musheng.common.context;

/**
 * 店铺上下文 - 使用 ThreadLocal 存储当前请求的店铺ID
 * <p>
 * 用于在整个请求处理链中传递当前店铺信息，实现类SaaS多租户数据隔离
 */
public class ShopContext {

    private static final ThreadLocal<Long> CURRENT_SHOP_ID = new ThreadLocal<>();

    /**
     * 设置当前店铺ID
     *
     * @param shopId 店铺ID
     */
    public static void setShopId(Long shopId) {
        CURRENT_SHOP_ID.set(shopId);
    }

    /**
     * 获取当前店铺ID
     *
     * @return 店铺ID，可能为null
     */
    public static Long getShopId() {
        return CURRENT_SHOP_ID.get();
    }

    /**
     * 获取当前店铺ID（必须存在）
     *
     * @return 店铺ID
     * @throws IllegalStateException 如果未设置店铺ID
     */
    public static Long requireShopId() {
        Long shopId = CURRENT_SHOP_ID.get();
        if (shopId == null) {
            throw new IllegalStateException("未选择店铺，请先选择店铺后再操作");
        }
        return shopId;
    }

    /**
     * 检查是否已设置店铺ID
     *
     * @return 是否已设置
     */
    public static boolean hasShopId() {
        return CURRENT_SHOP_ID.get() != null;
    }

    /**
     * 清除店铺上下文（请求结束后必须调用）
     */
    public static void clear() {
        CURRENT_SHOP_ID.remove();
    }
}
