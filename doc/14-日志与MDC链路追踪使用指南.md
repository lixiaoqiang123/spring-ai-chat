# 14-日志与MDC链路追踪使用指南

## 📋 概述

本项目已集成SLF4J日志框架和MDC（Mapped Diagnostic Context）链路追踪功能，可以在分布式环境下追踪完整的请求链路。

## 🎯 功能特性

### 1. SLF4J日志框架
- 使用业界标准的SLF4J + Logback日志框架
- 支持多种日志级别：TRACE、DEBUG、INFO、WARN、ERROR
- 支持参数化日志，避免字符串拼接
- 异步日志输出，提高性能

### 2. MDC链路追踪
- 自动为每个HTTP请求生成唯一的`traceId`
- 支持从请求头传递`traceId`，实现跨服务追踪
- 支持`sessionId`追踪，关联同一会话的多次请求
- 日志中自动包含`traceId`和`sessionId`，方便问题排查

### 3. 日志输出
- **控制台输出**：开发环境实时查看
- **文件输出**：所有日志记录到文件
- **错误日志**：ERROR级别单独记录
- **日志滚动**：按天滚动，单文件最大100MB
- **日志保留**：保留30天历史日志

## 🚀 快速开始

### 1. 在代码中使用日志

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class MyController {

    // 创建Logger实例
    private static final Logger log = LoggerFactory.getLogger(MyController.class);

    @GetMapping("/example")
    public String example() {
        // INFO级别日志
        log.info("处理请求 - 参数: {}", param);

        // WARN级别日志
        log.warn("警告信息: {}", message);

        // ERROR级别日志（包含异常堆栈）
        try {
            // 业务逻辑
        } catch (Exception e) {
            log.error("处理失败", e);
        }

        // DEBUG级别日志（仅开发环境输出）
        log.debug("调试信息: {}", debugInfo);

        return "success";
    }
}
```

### 2. 日志级别说明

| 级别 | 用途 | 示例场景 |
|------|------|----------|
| **ERROR** | 错误信息，需要立即处理 | 异常、系统故障 |
| **WARN** | 警告信息，可能存在问题 | 参数验证失败、降级处理 |
| **INFO** | 重要的业务流程信息 | 请求开始/结束、关键操作 |
| **DEBUG** | 调试信息，仅开发环境 | 变量值、中间状态 |
| **TRACE** | 最详细的追踪信息 | 方法调用链、详细流程 |

### 3. 参数化日志（推荐）

```java
// ✅ 推荐：使用参数化日志
log.info("用户登录 - userId: {}, username: {}", userId, username);

// ❌ 不推荐：字符串拼接
log.info("用户登录 - userId: " + userId + ", username: " + username);
```

**优势**：
- 避免不必要的字符串拼接
- 日志级别不满足时不会执行拼接操作
- 性能更好

## 🔍 MDC链路追踪

### 1. 自动追踪

MDC拦截器会自动为每个HTTP请求生成`traceId`，无需手动配置。

**日志输出示例**：
```
2025-12-24 10:30:15.123 [http-nio-8080-exec-1] INFO  c.l.s.chat.controller.ChatController [TraceId:a1b2c3d4e5f6g7h8] [SessionId:session-123] - 收到聊天请求 - 消息: 你好, 会话ID: session-123
2025-12-24 10:30:15.456 [http-nio-8080-exec-1] INFO  c.l.s.chat.service.ChatService [TraceId:a1b2c3d4e5f6g7h8] [SessionId:session-123] - 调用AI模型
2025-12-24 10:30:16.789 [http-nio-8080-exec-1] INFO  c.l.s.chat.controller.ChatController [TraceId:a1b2c3d4e5f6g7h8] [SessionId:session-123] - 聊天响应成功 - 会话ID: session-123
```

### 2. 跨服务追踪

如果需要在微服务之间传递`traceId`，可以在HTTP请求头中添加：

```java
// 客户端发送请求时
HttpHeaders headers = new HttpHeaders();
headers.set("X-Trace-Id", traceId);
headers.set("X-Session-Id", sessionId);

// 服务端会自动从请求头读取traceId和sessionId
```

### 3. 手动设置MDC

在某些场景下（如异步任务、消息队列），可以手动设置MDC：

```java
import org.slf4j.MDC;

public void processAsync() {
    // 设置traceId
    MDC.put("traceId", generateTraceId());
    MDC.put("sessionId", sessionId);

    try {
        // 业务逻辑
        log.info("异步任务执行");
    } finally {
        // 清理MDC，防止内存泄漏
        MDC.clear();
    }
}
```

## 📁 日志文件

### 1. 日志文件位置

```
logs/
├── spring-ai-chat.log              # 所有日志
├── spring-ai-chat.2025-12-23.0.log # 历史日志（按天滚动）
├── error.log                        # 错误日志
└── error.2025-12-23.0.log          # 历史错误日志
```

### 2. 日志滚动策略

- **按天滚动**：每天生成新的日志文件
- **按大小滚动**：单个文件超过100MB时自动分割
- **保留期限**：保留最近30天的日志
- **文件命名**：`spring-ai-chat.yyyy-MM-dd.序号.log`

### 3. 查看日志

```bash
# 查看最新日志
tail -f logs/spring-ai-chat.log

# 查看错误日志
tail -f logs/error.log

# 根据traceId搜索日志
grep "a1b2c3d4e5f6g7h8" logs/spring-ai-chat.log

# 根据sessionId搜索日志
grep "session-123" logs/spring-ai-chat.log
```

## ⚙️ 配置说明

### 1. Logback配置文件

配置文件位置：`src/main/resources/logback-spring.xml`

**关键配置**：

```xml
<!-- 日志格式（包含MDC信息） -->
<property name="CONSOLE_LOG_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [TraceId:%X{traceId}] [SessionId:%X{sessionId}] - %msg%n"/>

<!-- 项目日志级别 -->
<logger name="com.lxq.spring_api_chat" level="INFO"/>

<!-- 根日志级别 -->
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="ASYNC_FILE"/>
    <appender-ref ref="ASYNC_ERROR_FILE"/>
</root>
```

### 2. 环境配置

**开发环境（dev）**：
- 日志级别：DEBUG
- 输出：仅控制台
- 适合：本地开发调试

**生产环境（prod）**：
- 日志级别：WARN
- 输出：文件（异步）
- 适合：生产环境

### 3. 修改日志级别

**方式1：修改logback-spring.xml**

```xml
<!-- 修改项目日志级别 -->
<logger name="com.lxq.spring_api_chat" level="DEBUG"/>
```

**方式2：通过application.yaml**

```yaml
logging:
  level:
    com.lxq.spring_api_chat: DEBUG
    org.springframework: INFO
```

## 🔧 核心组件

### 1. MdcInterceptor

**位置**：`com.lxq.spring_api_chat.common.interceptor.MdcInterceptor`

**功能**：
- 在请求开始时生成或获取`traceId`
- 将`traceId`和`sessionId`放入MDC
- 在请求结束时清理MDC
- 将`traceId`添加到响应头

**关键代码**：
```java
// 生成traceId
String traceId = UUID.randomUUID().toString().replace("-", "");

// 放入MDC
MDC.put("traceId", traceId);
MDC.put("sessionId", sessionId);

// 清理MDC
MDC.clear();
```

### 2. WebMvcConfig

**位置**：`com.lxq.spring_api_chat.common.config.WebMvcConfig`

**功能**：
- 注册MDC拦截器
- 拦截所有HTTP请求

## 📊 最佳实践

### 1. 日志记录原则

✅ **应该记录**：
- 请求开始和结束
- 关键业务操作
- 异常和错误
- 重要的状态变化
- 性能指标

❌ **不应该记录**：
- 敏感信息（密码、密钥）
- 大量重复信息
- 过于详细的调试信息（生产环境）
- 循环中的日志（可能导致日志爆炸）

### 2. 异常日志

```java
// ✅ 推荐：记录完整异常堆栈
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("操作失败 - 参数: {}", param, e);
}

// ❌ 不推荐：只记录异常消息
catch (Exception e) {
    log.error("操作失败: " + e.getMessage());
}
```

### 3. 性能考虑

```java
// ✅ 推荐：使用日志级别判断
if (log.isDebugEnabled()) {
    log.debug("复杂计算结果: {}", expensiveOperation());
}

// ❌ 不推荐：直接调用（即使DEBUG级别关闭，也会执行expensiveOperation）
log.debug("复杂计算结果: {}", expensiveOperation());
```

### 4. 结构化日志

```java
// ✅ 推荐：结构化信息
log.info("用户操作 - userId: {}, action: {}, result: {}",
         userId, action, result);

// ❌ 不推荐：非结构化信息
log.info("用户" + userId + "执行了" + action + "，结果是" + result);
```

## 🐛 问题排查

### 1. 根据traceId追踪请求

```bash
# 搜索特定traceId的所有日志
grep "TraceId:a1b2c3d4e5f6g7h8" logs/spring-ai-chat.log

# 实时追踪
tail -f logs/spring-ai-chat.log | grep "TraceId:a1b2c3d4e5f6g7h8"
```

### 2. 根据sessionId追踪会话

```bash
# 搜索特定会话的所有请求
grep "SessionId:session-123" logs/spring-ai-chat.log
```

### 3. 查看错误日志

```bash
# 查看最近的错误
tail -100 logs/error.log

# 搜索特定错误
grep "NullPointerException" logs/error.log
```

## 📝 示例场景

### 场景1：追踪用户对话流程

**用户发起对话**：
```
2025-12-24 10:30:15.123 [http-nio-8080-exec-1] INFO  ChatController [TraceId:abc123] [SessionId:session-001] - 收到聊天请求 - 消息: 你好
```

**调用AI服务**：
```
2025-12-24 10:30:15.456 [http-nio-8080-exec-1] INFO  ChatService [TraceId:abc123] [SessionId:session-001] - 调用AI模型
```

**返回响应**：
```
2025-12-24 10:30:16.789 [http-nio-8080-exec-1] INFO  ChatController [TraceId:abc123] [SessionId:session-001] - 聊天响应成功
```

通过`traceId:abc123`可以追踪整个请求链路，通过`sessionId:session-001`可以关联同一会话的多次对话。

### 场景2：排查错误

**发现错误日志**：
```
2025-12-24 10:35:20.123 [http-nio-8080-exec-2] ERROR ChatService [TraceId:def456] [SessionId:session-002] - RAG检索失败
java.lang.NullPointerException: Cannot invoke "String.length()" because "query" is null
    at com.lxq.spring_api_chat.rag.service.RagService.search(RagService.java:45)
    ...
```

**根据traceId追踪完整流程**：
```bash
grep "TraceId:def456" logs/spring-ai-chat.log
```

可以看到该请求的完整处理过程，快速定位问题原因。

## 🎓 总结

1. **使用SLF4J**：统一的日志接口，便于切换实现
2. **参数化日志**：提高性能，避免字符串拼接
3. **MDC追踪**：自动生成traceId，方便问题排查
4. **合理级别**：根据重要性选择日志级别
5. **结构化信息**：便于日志分析和搜索
6. **异常处理**：记录完整堆栈信息
7. **性能优化**：使用异步日志，避免阻塞

## 📚 相关文档

- [Logback官方文档](http://logback.qos.ch/documentation.html)
- [SLF4J官方文档](http://www.slf4j.org/manual.html)
- [Spring Boot日志文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

---

**最后更新**: 2025-12-24
**维护者**: Claude Code Assistant
