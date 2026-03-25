package com.musheng.interceptor;

import com.musheng.common.context.ShopContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 店铺上下文拦截器
 * <p>
 * 从请求头中解析店铺ID并设置到 ThreadLocal 上下文中
 */
@Slf4j
@Component
public class ShopContextInterceptor implements HandlerInterceptor {

    /**
     * 店铺ID请求头名称
     */
    public static final String SHOP_ID_HEADER = "X-Shop-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String shopIdStr = request.getHeader(SHOP_ID_HEADER);
        
        if (StringUtils.hasText(shopIdStr)) {
            try {
                Long shopId = Long.parseLong(shopIdStr);
                ShopContext.setShopId(shopId);
                log.info("[ShopContext] 请求路径={}, shopId={}", request.getRequestURI(), shopId);
            } catch (NumberFormatException e) {
                log.warn("无效的店铺ID: {}", shopIdStr);
            }
        } else {
            log.warn("[ShopContext] 请求路径={}, 未携带 X-Shop-Id header", request.getRequestURI());
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清除 ThreadLocal，防止内存泄漏
        ShopContext.clear();
    }
}
