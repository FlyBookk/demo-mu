package com.musheng.aop;

import cn.dev33.satoken.stp.StpUtil;
import com.musheng.common.context.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 请求日志切面
 * <p>
 * 拦截所有 Controller 方法，记录请求入参、响应结果、耗时和 traceId。
 * 响应体超过 500 字符时截断，避免大数据量日志刷屏。
 *
 * @author wanhua
 * 2026年03月15日
 */
@Aspect
@Component
@Order(10)
public class RequestLogAspect {

    private static final Logger log = LoggerFactory.getLogger(RequestLogAspect.class);

    /** 响应体日志最大长度，超出截断 */
    private static final int MAX_RESPONSE_LENGTH = 500;

    /**
     * 切点：拦截所有带 @RestController 注解的类的方法
     * 覆盖 web/business/config/system 各模块下的 Controller
     *
     * @author wanhua
     * 2026年03月15日
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：记录请求和响应日志
     *
     * @param joinPoint 连接点
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     * @author wanhua
     * 2026年03月15日
     */
    @Around("controllerPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        String traceId = TraceContext.get();
        String method = "";
        String uri = "";
        String clientIp = "";

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
            clientIp = getClientIp(request);
        }

        // 获取当前登录用户（未登录时不抛异常）
        String userId = "未登录";
        try {
            if (StpUtil.isLogin()) {
                userId = String.valueOf(StpUtil.getLoginId());
            }
        } catch (Exception ignored) {
        }

        // 获取方法签名和参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();

        log.info("[请求开始] traceId={} userId={} {} {} ip={} 方法={}.{} 参数={}",
                traceId, userId, method, uri, clientIp, className, methodName,
                formatArgs(args));

        Object result;
        try {
            result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[请求结束] traceId={} {} {} 耗时={}ms 响应={}",
                    traceId, method, uri, elapsed, truncate(result));
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[请求异常] traceId={} {} {} 耗时={}ms 异常={}",
                    traceId, method, uri, elapsed, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 格式化方法参数，过滤掉 Servlet 相关对象避免序列化问题
     *
     * @param args 方法参数数组
     * @return 格式化后的参数字符串
     * @author wanhua
     * 2026年03月15日
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        try {
            // 过滤掉 HttpServletRequest/Response 等不可序列化对象
            Object[] filtered = Arrays.stream(args)
                    .map(arg -> {
                        if (arg == null) return "null";
                        String className = arg.getClass().getName();
                        if (className.startsWith("org.springframework.web")
                                || className.startsWith("jakarta.servlet")) {
                            return "[" + arg.getClass().getSimpleName() + "]";
                        }
                        return arg;
                    })
                    .toArray();
            return Arrays.toString(filtered);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    /**
     * 截断过长的响应体日志
     *
     * @param result 响应对象
     * @return 截断后的字符串
     * @author wanhua
     * 2026年03月15日
     */
    private String truncate(Object result) {
        if (result == null) {
            return "null";
        }
        String str = result.toString();
        if (str.length() > MAX_RESPONSE_LENGTH) {
            return str.substring(0, MAX_RESPONSE_LENGTH) + "...[已截断]";
        }
        return str;
    }

    /**
     * 获取客户端真实 IP（兼容 Nginx 反向代理）
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     * @author wanhua
     * 2026年03月15日
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
