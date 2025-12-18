# Chat Memory 使用指南

## 功能概述

Chat Memory 功能使 AI 能够记住对话历史，支持真正的多轮对话。通过 `sessionId` 区分不同用户会话，每个会话独立存储对话历史。

## 核心特性

- ✅ **自动记忆管理** - AI 自动记住对话历史
- ✅ **会话隔离** - 不同会话的记忆互不干扰
- ✅ **滑动窗口** - 自动管理消息数量（最多20条）
- ✅ **灵活控制** - 支持手动清除会话记忆

## API 使用示例

### 1. 基本对话（带记忆）

```bash
# 第一轮对话 - 告诉 AI 你的名字
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我的名字叫张三",
    "sessionId": "user-123"
  }'

# 响应示例
{
  "reply": "你好张三！很高兴认识你。有什么我可以帮助你的吗？",
  "sessionId": "user-123"
}

# 第二轮对话 - AI 会记住你的名字
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我的名字是什么？",
    "sessionId": "user-123"
  }'

# 响应示例
{
  "reply": "你的名字是张三。",
  "sessionId": "user-123"
}
```

### 2. 流式对话（带记忆）

```bash
# 使用 SSE 流式返回，同样支持记忆功能
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "继续我们之前的对话",
    "sessionId": "user-123"
  }'
```

### 3. 清除会话记忆

```bash
# 清除指定会话的记忆
curl -X DELETE http://localhost:8080/api/chat/memory/user-123

# 响应
会话 user-123 的记忆已清除

# 清除所有会话的记忆（谨慎使用）
curl -X DELETE http://localhost:8080/api/chat/memory/all
```

## 会话管理最佳实践

### 1. SessionId 生成策略

```java
// 方式1：使用用户ID作为 sessionId（推荐）
String sessionId = "user-" + userId;

// 方式2：为每次对话生成唯一ID
String sessionId = UUID.randomUUID().toString();

// 方式3：基于业务场景生成
String sessionId = "order-" + orderId + "-support";
```

### 2. 会话生命周期管理

```java
// 开始新对话
ChatRequest request = new ChatRequest("你好", "user-123");
ChatResponse response = chatService.chat(request);

// 继续对话（使用相同的 sessionId）
ChatRequest request2 = new ChatRequest("我刚才说了什么？", "user-123");
ChatResponse response2 = chatService.chat(request2);

// 结束对话时清除记忆
chatService.clearMemory("user-123");
```

### 3. 多用户场景

```java
// 用户A的对话
ChatRequest requestA = new ChatRequest("我喜欢苹果", "user-A");
chatService.chat(requestA);

// 用户B的对话
ChatRequest requestB = new ChatRequest("我喜欢香蕉", "user-B");
chatService.chat(requestB);

// 用户A询问 - AI 只记得用户A的对话
ChatRequest requestA2 = new ChatRequest("我喜欢什么水果？", "user-A");
ChatResponse responseA = chatService.chat(requestA2);
// 回复：你喜欢苹果

// 用户B询问 - AI 只记得用户B的对话
ChatRequest requestB2 = new ChatRequest("我喜欢什么水果？", "user-B");
ChatResponse responseB = chatService.chat(requestB2);
// 回复：你喜欢香蕉
```

## 配置说明

### Memory 配置

```java
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)  // 每个会话最多保存20条消息
                .build();
    }
}
```

### 调整记忆容量

```java
// 增加记忆容量（适合长对话）
.maxMessages(50)

// 减少记忆容量（节省内存）
.maxMessages(10)
```

## 注意事项

### 1. 内存使用

- 当前使用 `InMemoryChatMemoryRepository`，数据存储在内存中
- 应用重启后所有对话历史会丢失
- 生产环境建议使用持久化存储（Redis、数据库等）

### 2. 会话隔离

- 不同 `sessionId` 的对话完全隔离
- 确保为每个用户/对话分配唯一的 `sessionId`
- 避免使用固定的 `sessionId`，否则所有用户会共享记忆

### 3. 记忆清理

- 滑动窗口会自动清理超过 `maxMessages` 的旧消息
- 长时间不活跃的会话建议手动清除记忆
- 敏感对话结束后应立即清除记忆

## 测试示例

项目包含完整的测试用例，位于 `src/test/java/com/lxq/spring_api_chat/chat/ChatMemoryTest.java`：

```bash
# 运行所有 Memory 测试
mvn test -Dtest=ChatMemoryTest

# 运行单个测试
mvn test -Dtest=ChatMemoryTest#testBasicMemory
```

## 后续优化方向

1. **持久化存储**
   - 集成 Redis 作为 ChatMemoryRepository
   - 支持数据库存储（MySQL、PostgreSQL）
   - 实现会话过期机制

2. **高级功能**
   - 支持长期记忆和短期记忆分离
   - 实现记忆摘要和压缩
   - 添加记忆检索优化

3. **监控和管理**
   - 添加记忆使用统计
   - 实现记忆导出和导入
   - 提供记忆可视化界面

## 相关文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [项目 CLAUDE.md](CLAUDE.md)
- [项目 DOLIST.md](DOLIST.md)
