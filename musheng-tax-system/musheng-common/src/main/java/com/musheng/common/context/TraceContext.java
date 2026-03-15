package com.musheng.common.context;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪上下文
 * <p>
 * 管理每个请求的 traceId，存储在 ThreadLocal 和 MDC 中。
 * 请求结束后必须调用 {@link #clear()} 释放资源，防止线程池复用时串号。
 *
 * @author wanhua
 * 2026年03月15日
 */
public class TraceContext {

    /** MDC key，与 logback pattern 中的 %X{traceId} 对应 */
    public static final String TRACE_ID_KEY = "traceId";

    /** 请求头名称，支持上游系统透传 traceId */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final ThreadLocal<String> CURRENT_TRACE_ID = new ThreadLocal<>();

    /**
     * 生成并设置新的 traceId（UUID 去掉横线，取前16位，简洁可读）
     *
     * @return 生成的 traceId
     * @author wanhua
     * 2026年03月15日
     */
    public static String generateAndSet() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        set(traceId);
        return traceId;
    }

    /**
     * 设置指定 traceId（用于上游透传场景）
     *
     * @param traceId 链路追踪ID
     * @author wanhua
     * 2026年03月15日
     */
    public static void set(String traceId) {
        CURRENT_TRACE_ID.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 traceId
     *
     * @return traceId，可能为 null
     * @author wanhua
     * 2026年03月15日
     */
    public static String get() {
        return CURRENT_TRACE_ID.get();
    }

    /**
     * 清除 traceId（请求结束后必须调用，防止线程池复用时串号）
     *
     * @author wanhua
     * 2026年03月15日
     */
    public static void clear() {
        CURRENT_TRACE_ID.remove();
        MDC.remove(TRACE_ID_KEY);
    }
}
