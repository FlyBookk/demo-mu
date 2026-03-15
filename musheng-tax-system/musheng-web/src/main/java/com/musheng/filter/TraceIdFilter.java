package com.musheng.filter;

import com.musheng.common.context.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪过滤器
 * <p>
 * 每个请求进入时生成唯一 traceId 并写入 MDC，响应头中回写 traceId 方便前端排查问题。
 * 请求结束后在 finally 块中强制清除，确保线程池复用时不会串号。
 * <p>
 * 优先级设为最高（Order=1），在所有业务过滤器之前执行。
 *
 * @author wanhua
 * 2026年03月15日
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 响应头中回写 traceId 的 key */
    private static final String TRACE_ID_RESPONSE_HEADER = "X-Trace-Id";

    /**
     * 过滤器核心逻辑：生成 traceId → 执行请求链 → 清除 traceId
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
     * @author wanhua
     * 2026年03月15日
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 优先使用上游透传的 traceId，否则自动生成
            String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);
            if (StringUtils.hasText(traceId)) {
                TraceContext.set(traceId);
            } else {
                traceId = TraceContext.generateAndSet();
            }

            // 响应头回写 traceId，方便前端/调用方关联日志
            response.setHeader(TRACE_ID_RESPONSE_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            // 必须在 finally 中清除，确保异常情况下也能释放，防止线程池复用时串号
            TraceContext.clear();
        }
    }
}
