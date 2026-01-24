package com.musheng.aop;

import com.musheng.common.annotation.RequireShop;
import com.musheng.common.context.ShopContext;
import com.musheng.common.exception.BusinessException;
import com.musheng.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 店铺必填校验切面
 * <p>
 * 拦截所有带有 @RequireShop 注解的方法，校验是否已选择店铺
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class ShopRequiredAspect {

    /**
     * 拦截带有 @RequireShop 注解的方法
     */
    @Around("@annotation(com.musheng.common.annotation.RequireShop) || @within(com.musheng.common.annotation.RequireShop)")
    public Object checkShopRequired(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        RequireShop annotation = getRequireShopAnnotation(joinPoint);
        
        if (annotation != null) {
            // 检查店铺上下文
            Long shopId = ShopContext.getShopId();
            
            if (shopId == null) {
                String message = annotation.message();
                log.warn("访问业务接口但未选择店铺: method={}", 
                        joinPoint.getSignature().toShortString());
                throw new BusinessException(ErrorCode.SHOP_NOT_SELECTED, message);
            }
            
            log.debug("店铺校验通过: shopId={}, method={}", 
                    shopId, joinPoint.getSignature().toShortString());
        }
        
        return joinPoint.proceed();
    }

    /**
     * 获取 @RequireShop 注解（方法级优先，然后是类级）
     */
    private RequireShop getRequireShopAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 先检查方法级注解
        RequireShop methodAnnotation = method.getAnnotation(RequireShop.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        
        // 再检查类级注解
        return joinPoint.getTarget().getClass().getAnnotation(RequireShop.class);
    }
}
