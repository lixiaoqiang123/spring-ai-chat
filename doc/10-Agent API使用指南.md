# Agent API 使用指南

## 概述

Agent API 提供了一个智能代理接口，能够通过思考、规划、执行和反思的流程来完成用户任务。Agent 可以调用多种工具来解决复杂问题。

## 核心特性

### 1. 智能工作流
- **思考 (Thinking)**: 分析用户任务，理解需求
- **规划 (Planning)**: 制定解决方案步骤
- **执行 (Action)**: 使用可用工具执行任务
- **反思 (Reflection)**: 评估结果是否满足要求

### 2. 可用工具
- **Calculator**: 数学计算工具
- **Weather**: 天气查询工具
- **Search**: 信息搜索工具

### 3. 会话管理
- 支持会话ID，可追踪多轮对话
- 可配置最大执行步数，防止无限循环

## API 接口

### 1. 执行 Agent 任务

**接口**: `POST /api/agent/execute`

**请求体**:
```json
{
  "task": "用户任务描述",
  "sessionId": "会话ID（可选）",
  "maxSteps": 10
}
```

**参数说明**:
- `task` (必填): 用户任务描述
- `sessionId` (可选): 会话ID，用于追踪对话上下文
- `maxSteps` (可选): 最大执行步数，默认10步

**响应体**:
```json
{
  "sessionId": "会话ID",
  "finalAnswer": "最终答案",
  "steps": [
    {
      "stepNumber": 1,
      "type": "THINKING",
      "content": "思考内容",
      "toolName": null,
      "toolInput": null,
      "toolOutput": null
    },
    {
      "stepNumber": 2,
      "type": "TOOL_CALL",
      "content": "工具调用",
      "toolName": "Calculator",
      "toolInput": "25 * 4 + 10",
      "toolOutput": "110"
    }
  ],
  "success": true,
  "errorMessage": null,
  "totalTime": 1234
}
```

**响应字段说明**:
- `sessionId`: 会话ID
- `finalAnswer`: Agent 的最终答案
- `steps`: 执行步骤列表
  - `stepNumber`: 步骤编号
  - `type`: 步骤类型（THINKING, TOOL_CALL, REFLECTION, FINAL_ANSWER）
  - `content`: 步骤内容
  - `toolName`: 使用的工具名称（仅 TOOL_CALL 类型）
  - `toolInput`: 工具输入参数（仅 TOOL_CALL 类型）
  - `toolOutput`: 工具输出结果（仅 TOOL_CALL 类型）
- `success`: 执行是否成功
- `errorMessage`: 错误信息（如果失败）
- `totalTime`: 总执行时间（毫秒）

### 2. 简单查询接口

**接口**: `GET /api/agent/ask`

**请求参数**:
- `task`: 任务描述

**示例**:
```bash
GET /api/agent/ask?task=计算100加200
```

### 3. 健康检查

**接口**: `GET /api/agent/health`

**响应**: `Agent service is running`

## 使用示例

### 示例1: 数学计算

**请求**:
```bash
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "计算 25 * 4 + 10 的结果",
    "sessionId": "calc-session-1",
    "maxSteps": 10
  }'
```

**响应**:
```json
{
  "sessionId": "calc-session-1",
  "finalAnswer": "计算结果是 110",
  "steps": [
    {
      "stepNumber": 1,
      "type": "THINKING",
      "content": "用户需要计算 25 * 4 + 10，我需要使用计算器工具。USE_TOOL: Calculator 25 * 4 + 10"
    },
    {
      "stepNumber": 2,
      "type": "TOOL_CALL",
      "toolName": "Calculator",
      "toolInput": "25 * 4 + 10",
      "toolOutput": "110"
    },
    {
      "stepNumber": 3,
      "type": "REFLECTION",
      "content": "计算器返回了正确的结果 110，任务已完成。"
    },
    {
      "stepNumber": 4,
      "type": "FINAL_ANSWER",
      "content": "计算结果是 110"
    }
  ],
  "success": true,
  "errorMessage": null,
  "totalTime": 2345
}
```

### 示例2: 天气查询

**请求**:
```bash
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "查询北京的天气情况",
    "sessionId": "weather-session-1",
    "maxSteps": 10
  }'
```

### 示例3: 信息搜索

**请求**:
```bash
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "搜索 Spring AI 的相关信息",
    "sessionId": "search-session-1",
    "maxSteps": 10
  }'
```

### 示例4: 复杂任务（多工具协作）

**请求**:
```bash
curl -X POST http://localhost:8080/api/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "task": "先查询上海的天气，然后计算如果温度是18度，转换成华氏度是多少",
    "sessionId": "complex-session-1",
    "maxSteps": 15
  }'
```

这个任务需要 Agent：
1. 使用 Weather 工具查询上海天气
2. 使用 Calculator 工具进行温度转换（华氏度 = 摄氏度 * 9/5 + 32）
3. 综合两个工具的结果给出最终答案

## 工具说明

### Calculator 工具
- **功能**: 执行数学计算
- **输入**: 数学表达式字符串（如 "25 * 4 + 10"）
- **输出**: 计算结果

### Weather 工具
- **功能**: 查询城市天气
- **输入**: 城市名称（如 "北京"）
- **输出**: 天气信息（模拟数据）

### Search 工具
- **功能**: 搜索信息
- **输入**: 搜索关键词
- **输出**: 搜索结果（模拟数据）

## 错误处理

### 1. 空任务
**请求**:
```json
{
  "task": "",
  "sessionId": "error-session-1"
}
```

**响应**: HTTP 400 Bad Request

### 2. 执行失败
如果 Agent 执行过程中出现错误，响应中 `success` 字段为 `false`，`errorMessage` 包含错误信息。

```json
{
  "sessionId": "error-session-1",
  "finalAnswer": null,
  "steps": [...],
  "success": false,
  "errorMessage": "执行失败: 连接超时",
  "totalTime": 5000
}
```

## 最佳实践

### 1. 设置合理的 maxSteps
- 简单任务：5-10 步
- 复杂任务：10-20 步
- 避免设置过大的值，防止资源浪费

### 2. 使用会话ID
- 为相关的多轮对话使用相同的 sessionId
- 便于追踪和调试

### 3. 任务描述清晰
- 明确说明需要完成的任务
- 提供必要的上下文信息
- 避免模糊或歧义的表达

### 4. 监控执行时间
- 关注 `totalTime` 字段
- 对于耗时过长的任务，考虑优化或拆分

## 测试脚本

项目提供了测试脚本 `doc/sh/test-agent-api.sh`，可以快速测试所有接口功能。

**使用方法**:
```bash
# 确保应用已启动
cd doc/sh
chmod +x test-agent-api.sh
./test-agent-api.sh
```

## 技术实现

### 架构设计
```
AgentController (REST API)
    ↓
AgentService (核心逻辑)
    ↓
ChatClient (Spring AI)
    ↓
AgentTool (工具接口)
    ├── CalculatorTool
    ├── WeatherTool
    └── SearchTool
```

### 执行流程
1. 接收用户任务
2. 进入思考阶段，分析任务
3. 判断是否需要使用工具
4. 如果需要，调用相应工具
5. 对工具输出进行反思
6. 重复步骤2-5，直到得出最终答案或达到最大步数
7. 返回执行结果

### 关键特性
- 使用 Java 21 Record 定义 DTO
- 基于 Spring AI 的 ChatClient
- 可扩展的工具系统
- 完整的执行步骤追踪
- 错误处理和超时控制

## 扩展开发

### 添加新工具

1. 实现 `AgentTool` 接口:
```java
@Component
public class MyCustomTool implements AgentTool {

    @Override
    public String getName() {
        return "MyTool";
    }

    @Override
    public String getDescription() {
        return "我的自定义工具";
    }

    @Override
    public String execute(String input) {
        // 实现工具逻辑
        return "执行结果";
    }

    @Override
    public String getParameterDescription() {
        return "输入参数说明";
    }
}
```

2. Spring 会自动注册该工具到 AgentService

### 自定义 System Prompt

修改 `AgentService` 中的 `SYSTEM_PROMPT` 常量，调整 Agent 的行为模式。

## 相关文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [项目 CLAUDE.md](../../CLAUDE.md)
- [项目 DOLIST.md](../../DOLIST.md)

---

**最后更新**: 2025-12-16
**维护者**: Claude Code Assistant
