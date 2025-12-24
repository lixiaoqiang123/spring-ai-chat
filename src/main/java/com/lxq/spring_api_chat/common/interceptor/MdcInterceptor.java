package com.lxq.spring_api_chat.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * MDC拦截器 - 实现链路追踪
 * 为每个HTTP请求生成唯一的traceId，并将其放入MDC中
 * 这样可以在日志中追踪整个请求的处理过程
 */
public class MdcInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MdcInterceptor.class);

    /**
     * MDC中traceId的键名
     */
    public static final String TRACE_ID = "traceId";

    /**
     * MDC中sessionId的键名
     */
    public static final String SESSION_ID = "sessionId";

    /**
     * HTTP请求头中traceId的键名
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * HTTP请求头中sessionId的键名
     */
    public static final String SESSION_ID_HEADER = "X-Session-Id";

    /**
     * 在请求处理之前执行
     * 生成或获取traceId，并放入MDC
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // 从请求头获取traceId，如果没有则生成新的
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }

        // 从请求头获取sessionId（如果有）
        String sessionId = request.getHeader(SESSION_ID_HEADER);

        // 将traceId放入MDC
        MDC.put(TRACE_ID, traceId);

        // 如果有sessionId，也放入MDC
        if (sessionId != null && !sessionId.isBlank()) {
            MDC.put(SESSION_ID, sessionId);
        }

        // 将traceId添加到响应头，方便客户端追踪
        response.setHeader(TRACE_ID_HEADER, traceId);

        log.debug("请求开始 - URI: {}, Method: {}, TraceId: {}, SessionId: {}",
                request.getRequestURI(), request.getMethod(), traceId, sessionId);

        return true;
    }

    /**
     * 在请求处理完成后执行
     * 清理MDC，防止内存泄漏
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        String traceId = MDC.get(TRACE_ID);
        log.debug("请求完成 - URI: {}, Status: {}, TraceId: {}",
                request.getRequestURI(), response.getStatus(), traceId);

        // 清理MDC，防止内存泄漏
        MDC.remove(TRACE_ID);
        MDC.remove(SESSION_ID);
        MDC.clear();
    }

    /**
     * 生成唯一的traceId
     * 使用UUID的简化版本（去掉横线）
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
