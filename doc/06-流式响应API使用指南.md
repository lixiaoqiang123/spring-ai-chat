# Spring AI Chat - 流式响应 API 使用指南

## 概述

本项目已成功实现流式响应功能,使用 Spring AI 的 `.stream()` 方法和 Server-Sent Events (SSE) 技术,可以实时逐字返回 AI 生成的内容。

## 技术架构

### 后端实现

#### 1. Service 层 (ChatService.java)

**流式方法签名**:
```java
public Flux<String> chatStream(ChatRequest request)
```

**核心实现**:
```java
return chatClient.prompt()
        .user(request.getMessage())
        .stream()      // 使用Spring AI的stream()方法
        .content()     // 提取内容流
        .doOnError(error -> System.err.println("流式对话发生错误: " + error.getMessage()))
        .doOnComplete(() -> System.out.println("流式对话完成"));
```

**特性**:
- 返回 `Flux<String>` 响应式流
- 使用 Spring AI 的原生 `stream()` 方法
- 内置错误处理和日志记录
- 自动生成会话ID管理

#### 2. Controller 层 (ChatController.java)

**流式端点**:
- POST `/api/chat/stream` - 完整的流式对话接口
- GET `/api/chat/stream?message=xxx` - 简化的GET接口用于快速测试

**SSE 事件类型**:
1. **message** - AI生成的内容片段
2. **done** - 流结束事件,包含会话ID
3. **error** - 错误事件

**核心实现**:
```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamMessage(@RequestBody ChatRequest request) {
    return contentStream
            .map(content -> ServerSentEvent.<String>builder()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .event("message")
                    .data(content)
                    .build())
            .concatWith(doneEvent)
            .onErrorResume(error -> errorEvent)
            .timeout(Duration.ofMinutes(5));
}
```

## API 端点

### 1. 流式对话 (POST)

**端点**: `POST /api/chat/stream`

**请求头**:
```
Content-Type: application/json
```

**请求体**:
```json
{
  "message": "你好,请介绍一下 Spring AI",
  "sessionId": "optional-session-id"
}
```

**响应**: `text/event-stream`

**SSE 事件格式**:
```
event: message
id: 1701234567890
data: 你好

event: message
id: 1701234567891
data: !

event: message
id: 1701234567892
data: Spring AI 是...

event: done
id: 1701234567900
data: {"sessionId":"uuid-here"}
```

### 2. 流式对话 (GET) - 简化版

**端点**: `GET /api/chat/stream?message=<your-message>`

**示例**:
```
GET http://localhost:8080/api/chat/stream?message=你好
```

**响应**: 与POST相同的SSE格式

### 3. 传统非流式接口 (保持向后兼容)

#### POST /api/chat/send
```json
{
  "message": "你好",
  "sessionId": "optional"
}
```

**响应**:
```json
{
  "reply": "你好!有什么可以帮助你的吗?",
  "sessionId": "uuid",
  "timestamp": 1701234567890
}
```

#### GET /api/chat/ask
```
GET /api/chat/ask?message=你好
```

## 客户端集成示例

### 1. JavaScript (Fetch API)

```javascript
async function sendStreamMessage(message) {
    const response = await fetch('http://localhost:8080/api/chat/stream', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            message: message,
            sessionId: sessionId
        })
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let fullResponse = '';

    while (true) {
        const {value, done} = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, {stream: true});
        const lines = buffer.split('\n');
        buffer = lines.pop();

        for (const line of lines) {
            if (line.startsWith('data:')) {
                const data = line.substring(5).trim();
                fullResponse += data;
                // 更新UI显示
                updateChatDisplay(fullResponse);
            }
        }
    }
}
```

### 2. JavaScript (EventSource) - 仅支持GET

```javascript
const eventSource = new EventSource(
    'http://localhost:8080/api/chat/stream?message=你好'
);

eventSource.addEventListener('message', (event) => {
    console.log('收到内容:', event.data);
    appendToChat(event.data);
});

eventSource.addEventListener('done', (event) => {
    const data = JSON.parse(event.data);
    console.log('会话ID:', data.sessionId);
    eventSource.close();
});

eventSource.addEventListener('error', (event) => {
    console.error('错误:', event.data);
    eventSource.close();
});
```

### 3. Java (WebClient)

```java
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

WebClient client = WebClient.create("http://localhost:8080");

Flux<String> stream = client.post()
        .uri("/api/chat/stream")
        .bodyValue(new ChatRequest("你好", null))
        .retrieve()
        .bodyToFlux(String.class);

stream.subscribe(
    content -> System.out.print(content),
    error -> System.err.println("错误: " + error),
    () -> System.out.println("\n对话完成")
);
```

### 4. Python (requests + SSE)

```python
import requests
import json

def stream_chat(message, session_id=None):
    url = "http://localhost:8080/api/chat/stream"
    headers = {"Content-Type": "application/json"}
    data = {
        "message": message,
        "sessionId": session_id
    }

    with requests.post(url, json=data, stream=True, headers=headers) as response:
        for line in response.iter_lines():
            if line:
                line = line.decode('utf-8')
                if line.startswith('data:'):
                    content = line[5:].strip()
                    print(content, end='', flush=True)

# 使用
stream_chat("介绍一下 Spring AI")
```

### 5. cURL 测试

```bash
# POST 请求
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"你好","sessionId":null}'

# GET ��求
curl -N "http://localhost:8080/api/chat/stream?message=你好"
```

## 测试页面

项目包含一个完整的测试页面,展示流式响应的实际效果:

**访问地址**: `http://localhost:8080/stream-test.html`

**功能特性**:
- 实时逐字显示AI回复
- 美观的聊天界面
- 打字指示器动画
- 自动会话管理
- 错误处理和显示

## 性能特性

### 1. 响应式编程
- 使用 Reactor Flux 实现非阻塞流式处理
- 支持背压 (Backpressure) 机制
- 自动资源管理

### 2. Virtual Threads (Java 21)
- 配合 Spring Boot 3.5 的 Virtual Threads 支持
- 高并发场景下性能优异
- 低内存占用

### 3. 超时和错误处理
- 5分钟超时保护
- 优雅的错误传播
- 自动资源清理

## 最佳实践

### 1. 会话管理
```java
// 保持会话ID以维持上下文
String sessionId = chatService.getOrGenerateSessionId(request);
```

### 2. 错误处理
```java
stream.onErrorResume(error -> {
    // 返回友好的错误消息
    return Flux.just(ServerSentEvent.<String>builder()
            .event("error")
            .data("错误: " + error.getMessage())
            .build());
});
```

### 3. 超时设置
```java
stream.timeout(Duration.ofMinutes(5));
```

### 4. 客户端重连
```javascript
// EventSource 自动重连
eventSource.onerror = (error) => {
    if (eventSource.readyState === EventSource.CLOSED) {
        // 重新连接逻辑
    }
};
```

## 配置要求

### application.yml / application.properties

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      chat:
        options:
          model: gpt-3.5-turbo
          temperature: 0.7
          stream: true  # 启用流式响应
```

### 依赖要求

```xml
<!-- Spring WebFlux (响应式支持) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Spring AI OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Reactor Core -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
</dependency>
```

## 故障排查

### 1. 流未返回内容
- 检查 API Key 配置
- 确认 base-url 正确
- 查看控制台错误日志

### 2. 连接超时
- 增加超时时间 `.timeout(Duration.ofMinutes(10))`
- 检查网络连接
- 验证代理设置

### 3. 前端解析失败
- 确认使用正确的 Content-Type: `text/event-stream`
- 检查 CORS 配置
- 验证 SSE 事件格式

### 4. 内存泄漏
- 确保客户端正确��闭连接
- 使用 `.timeout()` 防止无限等待
- 监控 Flux 订阅状态

## 监控和日志

### 日志配置

```yaml
logging:
  level:
    com.lxq.spring_api_chat: DEBUG
    org.springframework.ai: DEBUG
    reactor: INFO
```

### 监控端点

```java
// 在 Service 中添加监控
.doOnNext(content -> metrics.incrementContentChunks())
.doOnComplete(() -> metrics.recordStreamComplete())
.doOnError(error -> metrics.recordStreamError())
```

## 进阶功能

### 1. 添加元数据
```java
ServerSentEvent.<String>builder()
        .id(messageId)
        .event("message")
        .data(content)
        .comment("metadata: {...}")  // 添加元数据
        .retry(Duration.ofSeconds(10))  // 重试间隔
        .build()
```

### 2. 多路复用
```java
Flux<String> stream1 = chatService.chatStream(request1);
Flux<String> stream2 = chatService.chatStream(request2);
Flux<String> merged = Flux.merge(stream1, stream2);
```

### 3. 流控制
```java
stream
        .limitRate(10)  // 限制请求速率
        .buffer(5)      // 批量处理
        .delayElements(Duration.ofMillis(100));  // 添加延迟
```

## 总结

流式响应功能已完整实现,具备以下优势:

1. **实时体验**: 逐字返回,用户体验更好
2. **高性能**: 基于响应式编程,支持高并发
3. **向后兼容**: 保留原有非流式接口
4. **易于集成**: 标准 SSE 协议,支持多种客户端
5. **生产就绪**: 完善的错误处理和超时机制

---

**最后更新**: 2025-12-05
**Spring Boot 版本**: 3.5.9-SNAPSHOT
**Java 版本**: 21
