package com.musheng.system.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musheng.common.annotation.OperationLog;
import com.musheng.system.entity.User;
import com.musheng.system.mapper.UserMapper;
import com.musheng.system.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 操作日志切面
 * 拦截带有 @OperationLog 注解的方法，自动记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final LogService logService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知，拦截带有 @OperationLog 注解的方法
     */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 创建日志实体
        com.musheng.system.entity.OperationLog logEntity = new com.musheng.system.entity.OperationLog();
        
        // 设置模块和操作类型
        logEntity.setModule(operationLog.module());
        logEntity.setOperation(operationLog.operation());
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
            logEntity.setUserAgent(request.getHeader("User-Agent"));
        }
        
        // 获取当前用户信息
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                logEntity.setUserId(userId);
                
                // 获取用户名
                User user = userMapper.selectById(userId);
                if (user != null) {
                    logEntity.setUsername(user.getUsername());
                }
            }
        } catch (Exception e) {
            log.warn("获取当前用户信息失败: {}", e.getMessage());
        }
        
        // 获取请求参数
        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                // 过滤掉不能序列化的参数
                Object[] filteredArgs = filterArgs(args);
                if (filteredArgs.length > 0) {
                    String params = objectMapper.writeValueAsString(filteredArgs);
                    // 限制参数长度，避免过长
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000) + "...";
                    }
                    logEntity.setRequestParams(params);
                }
            }
        } catch (Exception e) {
            log.warn("序列化请求参数失败: {}", e.getMessage());
        }
        
        Object result = null;
        try {
            // 执行目标方法
            result = point.proceed();
            
            // 记录成功状态
            logEntity.setStatus(1);
            
            // 记录响应数据
            try {
                if (result != null) {
                    String responseData = objectMapper.writeValueAsString(result);
                    // 限制响应数据长度
                    if (responseData.length() > 2000) {
                        responseData = responseData.substring(0, 2000) + "...";
                    }
                    logEntity.setResponseData(responseData);
                }
            } catch (Exception e) {
                log.warn("序列化响应数据失败: {}", e.getMessage());
            }
            
            return result;
        } catch (Throwable e) {
            // 记录失败状态和错误信息
            logEntity.setStatus(0);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500) + "...";
            }
            logEntity.setErrorMsg(errorMsg);
            throw e;
        } finally {
            // 记录执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            logEntity.setExecutionTime(executionTime);
            
            // 异步保存日志
            try {
                logService.save(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 过滤不能序列化的参数
     */
    private Object[] filterArgs(Object[] args) {
        Object[] filteredArgs = new Object[args.length];
        int count = 0;
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            // 过滤掉 MultipartFile、HttpServletRequest、HttpServletResponse 等
            if (arg instanceof MultipartFile) {
                filteredArgs[count++] = "[File: " + ((MultipartFile) arg).getOriginalFilename() + "]";
            } else if (arg instanceof HttpServletRequest) {
                continue;
            } else if (arg instanceof jakarta.servlet.http.HttpServletResponse) {
                continue;
            } else {
                filteredArgs[count++] = arg;
            }
        }
        
        // 返回实际有效的参数
        Object[] result = new Object[count];
        System.arraycopy(filteredArgs, 0, result, 0, count);
        return result;
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
