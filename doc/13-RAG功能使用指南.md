# 13-RAG功能使用指南

> **文档版本**: v1.0
> **创建日期**: 2025-12-22
> **适用环境**: Spring Boot 3.5.9 + Spring AI 1.1.0 + JDK 21

---

## 📋 目录

- [1. RAG功能概述](#1-rag功能概述)
- [2. 快速开始](#2-快速开始)
- [3. API详细说明](#3-api详细说明)
- [4. 测试脚本](#4-测试脚本)
- [5. 最佳实践](#5-最佳实践)
- [6. 常见问题FAQ](#6-常见问题faq)
- [7. 故障排查](#7-故障排查)

---

## 1. RAG功能概述

### 1.1 什么是RAG

**RAG（Retrieval-Augmented Generation，检索增强生成）** 是一种结合信息检索和生成式AI的技术。它通过以下流程工作：

```
用户查询 → 向量化 → 检索相关文档 → 将文档作为上下文 → LLM生成答案
```

### 1.2 RAG的优势

| 优势 | 说明 |
|------|------|
| **准确性提升** | 基于真实文档生成答案，减少幻觉 |
| **知识实时更新** | 无需重新训练模型，只需更新文档 |
| **可追溯性** | 可以追踪答案来源，提供引用 |
| **领域专业性** | 支持企业私有知识库 |

### 1.3 本项目的RAG实现

本项目实现了完整的RAG功能栈：

- ✅ **文档索引**: 支持PDF、Markdown、TXT格式
- ✅ **向量存储**: 使用SimpleVectorStore（内存+JSON持久化）
- ✅ **相似度检索**: 基于Embedding的语义搜索
- ✅ **RAG对话**: 集成到Chat API，支持同步和流式响应
- ✅ **参数可配置**: topK、similarityThreshold可调整

---

## 2. 快速开始

### 2.1 启动应用

```bash
# 方式1: Maven命令启动
mvn spring-boot:run

# 方式2: IDE启动
# 运行 SpringApiChatApplication 的 main 方法
```

应用启动后，监听端口 `8080`。

### 2.2 准备文档

将需要索引的文档放到 `data/documents` 目录下：

```bash
# 创建文档目录
mkdir -p data/documents

# 复制文档（支持PDF、MD、TXT格式）
cp your-document.pdf data/documents/
cp your-document.md data/documents/
```

### 2.3 索引文档

使用 `/api/rag/index` 端点索引单个文档：

```bash
curl -X POST "http://localhost:8080/api/rag/index?filePath=your-document.md"
```

或使用 `/api/rag/index-directory` 批量索引整个目录：

```bash
curl -X POST "http://localhost:8080/api/rag/index-directory?directoryPath=data/documents"
```

**示例响应**：

```json
{
  "filename": "your-document.md",
  "success": true,
  "originalDocuments": 1,
  "chunksCreated": 15,
  "duration": 1250,
  "errorMessage": null
}
```

### 2.4 RAG查询

索引完成后，使用 `/api/chat/rag` 进行RAG增强对话：

```bash
curl -X POST "http://localhost:8080/api/chat/rag?topK=5&similarityThreshold=0.7" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请介绍一下Spring AI的QuestionAnswerAdvisor？",
    "sessionId": null
  }'
```

**示例响应**：

```json
{
  "reply": "QuestionAnswerAdvisor是Spring AI提供的一个Advisor组件，用于实现RAG功能...",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 3. API详细说明

### 3.1 文档索引API

#### 3.1.1 索引单个文档

**端点**: `POST /api/rag/index`

**参数**:
- `filePath` (必需): 文档路径，支持：
  - 绝对路径: `/home/user/docs/file.pdf`
  - 相对路径: `my-doc.md` (相对于 `data/documents`)

**请求示例**:

```bash
# 使用相对路径
curl -X POST "http://localhost:8080/api/rag/index?filePath=spring-ai-guide.md"

# 使用绝对路径
curl -X POST "http://localhost:8080/api/rag/index?filePath=/home/user/docs/manual.pdf"
```

**响应格式**:

```json
{
  "filename": "spring-ai-guide.md",
  "success": true,
  "originalDocuments": 1,
  "chunksCreated": 12,
  "duration": 850,
  "errorMessage": null
}
```

**状态码**:
- `200 OK`: 索引成功
- `400 Bad Request`: 文件不存在或不是有效文件
- `403 Forbidden`: 路径不安全（路径遍历攻击）
- `500 Internal Server Error`: 索引失败

#### 3.1.2 批量索引目录

**端点**: `POST /api/rag/index-directory`

**参数**:
- `directoryPath` (必需): 目录路径

**请求示例**:

```bash
curl -X POST "http://localhost:8080/api/rag/index-directory?directoryPath=data/documents"
```

**响应格式**: 返回索引结果列表

```json
[
  {
    "filename": "doc1.md",
    "success": true,
    "chunksCreated": 10,
    ...
  },
  {
    "filename": "doc2.pdf",
    "success": true,
    "chunksCreated": 20,
    ...
  }
]
```

### 3.2 RAG查询API

#### 3.2.1 纯检索API

**端点**: `POST /api/rag/query`

**功能**: 只执行相似度检索，不调用LLM生成答案

**请求体**:

```json
{
  "query": "什么是Spring AI？",
  "topK": 5,
  "similarityThreshold": 0.7
}
```

**参数说明**:
- `query` (必需): 查询文本
- `topK` (可选): 返回前K个结果，默认5，范围1-50
- `similarityThreshold` (可选): 相似度阈值，默认0.7，范围0.0-1.0

**请求示例**:

```bash
curl -X POST "http://localhost:8080/api/rag/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Spring AI的核心组件有哪些？",
    "topK": 3,
    "similarityThreshold": 0.6
  }'
```

**响应格式**:

```json
{
  "query": "Spring AI的核心组件有哪些？",
  "documents": [
    {
      "content": "ChatClient 是 Spring AI 的核心接口...",
      "source": "spring-ai-guide.md",
      "score": 0.85,
      "metadata": {
        "source": "spring-ai-guide.md",
        "indexedAt": "2025-12-22T10:30:00",
        "docType": "MARKDOWN"
      }
    }
  ],
  "documentCount": 3,
  "context": "ChatClient 是 Spring AI 的核心接口...\n\n..."
}
```

#### 3.2.2 RAG增强对话API

**端点**: `POST /api/chat/rag`

**功能**: 检索相关文档后，使用LLM生成答案

**请求参数**:
- URL参数:
  - `topK` (可选): 检索文档数量，默认5
  - `similarityThreshold` (可选): 相似度阈值，默认0.7

- 请求体:
  ```json
  {
    "message": "用户查询",
    "sessionId": "会话ID（可选）"
  }
  ```

**请求示例**:

```bash
curl -X POST "http://localhost:8080/api/chat/rag?topK=5&similarityThreshold=0.7" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请详细介绍Spring AI的QuestionAnswerAdvisor",
    "sessionId": null
  }'
```

**响应格式**:

```json
{
  "reply": "QuestionAnswerAdvisor是Spring AI提供的核心组件之一...",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**特性**:
- ✅ 自动检索相关文档
- ✅ 将文档内容注入到Prompt上下文
- ✅ 支持对话记忆（通过sessionId）
- ✅ 如果没有相关文档，会明确告知用户

#### 3.2.3 RAG增强流式对话API

**端点**: `POST /api/chat/rag-stream`

**功能**: 以SSE流式方式返回RAG增强的对话

**请求参数**: 同 `/api/chat/rag`

**请求示例**:

```bash
curl -X POST "http://localhost:8080/api/chat/rag-stream?topK=5&similarityThreshold=0.7" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请介绍Java 21的新特性",
    "sessionId": null
  }' \
  --no-buffer
```

**响应格式**: Server-Sent Events (SSE)

```
event: message
data: Java

event: message
data:  21

event: message
data:  引入了

...

event: done
data: {"sessionId":"550e8400-e29b-41d4-a716-446655440000"}
```

**前端接收示例**:

```javascript
const eventSource = new EventSource('http://localhost:8080/api/chat/rag-stream?topK=5');

eventSource.addEventListener('message', (e) => {
  console.log('收到内容:', e.data);
  // 累加显示
});

eventSource.addEventListener('done', (e) => {
  const { sessionId } = JSON.parse(e.data);
  console.log('对话完成，会话ID:', sessionId);
  eventSource.close();
});

eventSource.addEventListener('error', (e) => {
  console.error('错误:', e.data);
  eventSource.close();
});
```

### 3.3 统计信息API

**端点**: `GET /api/rag/stats`

**功能**: 获取RAG系统的统计信息

**请求示例**:

```bash
curl -X GET "http://localhost:8080/api/rag/stats"
```

**响应格式**:

```json
{
  "vectorStoreSize": 125,
  "vectorStorePath": "data/vectorstore/simple-vector-store.json",
  "embeddingModel": "text-embedding-3-small",
  "chunkSize": 500,
  "chunkOverlap": 100,
  "timestamp": "2025-12-22T14:30:00"
}
```

**字段说明**:
- `vectorStoreSize`: 向量存储中的文档块数量
- `vectorStorePath`: 持久化文件路径
- `embeddingModel`: 使用的Embedding模型
- `chunkSize`: 文档分块大小（tokens）
- `chunkOverlap`: 分块重叠大小（tokens）
- `timestamp`: 统计时间

---

## 4. 测试脚本

### 4.1 索引文档测试

创建测试脚本 `doc/sh/test-rag-index.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/rag"

echo "=== RAG文档索引测试 ==="

# 测试1: 索引单个文档
echo ""
echo "测试1: 索引单个Markdown文档"
curl -X POST "${BASE_URL}/index?filePath=README.md" | jq '.'

# 测试2: 批量索引目录
echo ""
echo "测试2: 批量索引文档目录"
curl -X POST "${BASE_URL}/index-directory?directoryPath=data/documents" | jq '.'

# 测试3: 获取统计信息
echo ""
echo "测试3: 获取统计信息"
curl -X GET "${BASE_URL}/stats" | jq '.'

echo ""
echo "=== 索引测试完成 ==="
```

### 4.2 RAG对话测试

创建测试脚本 `doc/sh/test-rag-chat.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api/chat"

echo "=== RAG对话测试 ==="

# 测试1: 基于文档的问答
echo ""
echo "测试1: RAG增强对话"
curl -X POST "${BASE_URL}/rag?topK=5&similarityThreshold=0.7" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请介绍一下Spring AI的核心组件？",
    "sessionId": null
  }' | jq '.'

# 测试2: 多轮对话（使用会话ID）
echo ""
echo "测试2: 多轮RAG对话"
SESSION_ID=$(uuidgen)
echo "会话ID: ${SESSION_ID}"

curl -X POST "${BASE_URL}/rag?topK=3" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": \"什么是Virtual Threads？\",
    \"sessionId\": \"${SESSION_ID}\"
  }" | jq '.'

sleep 1

curl -X POST "${BASE_URL}/rag?topK=3" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": \"它有什么优势？\",
    \"sessionId\": \"${SESSION_ID}\"
  }" | jq '.'

# 测试3: 流式RAG对话
echo ""
echo "测试3: 流式RAG对话"
curl -X POST "${BASE_URL}/rag-stream?topK=5" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "请总结一下Java 21的主要新特性",
    "sessionId": null
  }' \
  --no-buffer

echo ""
echo "=== RAG对话测试完成 ==="
```

### 4.3 批量操作测试

创建测试脚本 `doc/sh/test-rag-batch.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api"

echo "=== RAG批量操作测试 ==="

# 准备测试数据
echo "准备测试数据..."
mkdir -p data/test-docs

cat > data/test-docs/doc1.md << 'EOF'
# Spring AI简介
Spring AI是Spring团队推出的AI集成框架...
EOF

cat > data/test-docs/doc2.md << 'EOF'
# Java 21新特性
Virtual Threads是Java 21的重要特性...
EOF

# 批量索引
echo ""
echo "批量索引测试文档..."
curl -X POST "${BASE_URL}/rag/index-directory?directoryPath=data/test-docs" | jq '.'

# 批量查询测试
echo ""
echo "批量查询测试..."
QUERIES=("Spring AI" "Virtual Threads" "Java 21")

for query in "${QUERIES[@]}"; do
  echo ""
  echo "查询: ${query}"
  curl -X POST "${BASE_URL}/rag/query" \
    -H "Content-Type: application/json" \
    -d "{
      \"query\": \"${query}\",
      \"topK\": 3,
      \"similarityThreshold\": 0.6
    }" | jq '.documentCount'
done

# 清理
echo ""
echo "清理测试数据..."
rm -rf data/test-docs

echo ""
echo "=== 批量操作测试完成 ==="
```

### 4.4 运行集成测试

```bash
# 运行Spring Boot集成测试
mvn test -Dtest=RagIntegrationTest

# 或者在IDE中运行RagIntegrationTest类
```

---

## 5. 最佳实践

### 5.1 文档准备技巧

#### 文档格式选择

| 格式 | 优势 | 适用场景 |
|------|------|----------|
| **Markdown** | 结构清晰，易于编辑 | 技术文档、API文档、知识库 |
| **PDF** | 格式丰富，兼容性好 | 报告、手册、书籍 |
| **TXT** | 简单轻量 | 日志、配置说明 |

#### 文档质量要求

1. **结构化**: 使用标题、列表、表格组织内容
2. **完整性**: 确保信息完整，避免片段化
3. **准确性**: 核实信息准确无误
4. **最新性**: 定期更新过时内容

#### 文档大小建议

- **单个文档**: 建议 < 10MB
- **文档总量**: SimpleVectorStore 建议 < 1000个文档
- **文档数量过多**: 考虑升级到生产级VectorStore（Redis、Pinecone）

### 5.2 参数调优建议

#### ChunkSize和ChunkOverlap

当前配置（可在`application.yaml`中调整）：

```yaml
rag:
  chunking:
    size: 500          # 每块500个token
    overlap: 100       # 重叠100个token
```

**调优建议**：

| 文档类型 | ChunkSize | ChunkOverlap | 原因 |
|---------|-----------|--------------|------|
| 技术文档 | 500-800 | 100-150 | 需要完整的代码示例和解释 |
| 长文本 | 800-1000 | 150-200 | 保持段落完整性 |
| 短问答 | 300-500 | 50-100 | 快速定位答案 |
| 代码注释 | 200-400 | 50 | 精确匹配 |

#### TopK参数

**topK** 控制返回的文档块数量。

**推荐值**：

```
topK = 3-5（默认5）
```

**调整原则**：
- **topK太小**（<3）：可能遗漏重要信息
- **topK太大**（>10）：引入噪音，影响生成质量，且增加token消耗

**场景建议**：
- 精确查询（如API文档）: topK=3
- 综合查询（如概念解释）: topK=5
- 广泛查询（如全面了解）: topK=8

#### SimilarityThreshold参数

**similarityThreshold** 过滤低相关性结果。

**推荐值**：

```
similarityThreshold = 0.6-0.8（默认0.7）
```

**调整原则**：
- **阈值太低**（<0.5）：返回大量不相关结果
- **阈值太高**（>0.9）：可能过滤掉有用信息

**场景建议**：
- 精确匹配: 0.8-0.9
- 语义相关: 0.6-0.7
- 广泛搜索: 0.4-0.6

### 5.3 性能优化建议

#### 1. 索引优化

```java
// 批量索引时，考虑异步处理
@Async
public CompletableFuture<IndexResponse> indexDocumentAsync(Resource resource) {
    return CompletableFuture.completedFuture(indexDocument(resource));
}
```

#### 2. 缓存检索结果

对于热门查询，可以缓存检索结果：

```java
@Cacheable(value = "ragCache", key = "#query + '_' + #topK")
public List<Document> cachedSearch(String query, int topK) {
    return vectorStore.similaritySearch(...);
}
```

#### 3. 持久化策略

- **开发环境**: 每次索引后立即持久化
- **生产环境**: 定时持久化（如每5分钟）

```java
@Scheduled(fixedRate = 300000) // 5分钟
public void scheduledPersistence() {
    indexingService.persistVectorStore();
}
```

#### 4. 升级VectorStore

SimpleVectorStore适合开发和小规模使用。生产环境建议升级：

```
SimpleVectorStore (当前)
    ↓
Redis VectorStore (中等规模)
    ↓
Pinecone/Qdrant/Milvus (大规模)
```

### 5.4 安全建议

#### 1. 文件路径验证

API已内置路径安全检查，确保：
- 防止路径遍历攻击（`../../../etc/passwd`）
- 限制访问范围（只允许`data/`目录）

#### 2. 敏感信息过滤

索引前检查文档是否包含：
- API密钥
- 密码
- 个人身份信息

```java
// 建议添加敏感信息检测
public boolean containsSensitiveInfo(String content) {
    // 检查API密钥模式
    Pattern apiKeyPattern = Pattern.compile("api[_-]?key[\\s:=]+[\\w-]+",
        Pattern.CASE_INSENSITIVE);
    return apiKeyPattern.matcher(content).find();
}
```

#### 3. 访问控制

根据需要添加API认证：

```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/index")
public ResponseEntity<IndexResponse> indexDocument(...) {
    ...
}
```

---

## 6. 常见问题FAQ

### Q1: 索引文档后，查询不到结果？

**可能原因**：
1. **相似度阈值太高** - 降低`similarityThreshold`到0.5-0.6
2. **查询表述不准确** - 使用与文档相近的措辞
3. **文档未成功索引** - 检查索引响应的`success`字段

**解决方案**：

```bash
# 1. 检查统计信息
curl http://localhost:8080/api/rag/stats

# 2. 降低阈值重试
curl -X POST "http://localhost:8080/api/rag/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "你的查询",
    "topK": 10,
    "similarityThreshold": 0.4
  }'
```

### Q2: RAG回答质量不高？

**可能原因**：
1. 文档质量差（信息不完整、结构混乱）
2. ChunkSize设置不当（太大或太小）
3. topK太小，遗漏关键信息

**解决方案**：
1. 改进文档质量，确保结构化和完整性
2. 调整ChunkSize（见5.2节）
3. 增加topK到8-10

### Q3: 应用重启后，之前索引的文档丢失？

**原因**: 向量存储未正确持久化

**解决方案**：

```bash
# 检查持久化文件是否存在
ls -lh data/vectorstore/simple-vector-store.json

# 如果文件不存在，确保索引后调用持久化
# RagController已自动调用persistVectorStore()
```

### Q4: 如何清空向量存储，重新索引？

**方案1**: 删除持久化文件

```bash
rm data/vectorstore/simple-vector-store.json
# 重启应用
```

**方案2**: 代码清空（需要添加API）

```java
@DeleteMapping("/vector-store")
public ResponseEntity<String> clearVectorStore() {
    // 清空逻辑
    vectorStore.delete(/* all documents */);
    return ResponseEntity.ok("向量存储已清空");
}
```

### Q5: 支持哪些文档格式？

当前支持：
- ✅ PDF (`.pdf`)
- ✅ Markdown (`.md`)
- ✅ 纯文本 (`.txt`)

**扩展其他格式**：

项目已依赖`spring-ai-tika-document-reader`，理论上支持Tika能解析的所有格式（Word、Excel、PPT等），只需在`DocumentLoaderFactory`中添加对应的loader。

### Q6: SimpleVectorStore的性能瓶颈在哪里？

**限制**：
- 纯内存存储，重启后需重新加载
- 不支持分布式
- 检索速度随文档数量增长而下降
- 建议文档块数量 < 10000

**升级建议**：
- 1000-10000块: Redis VectorStore
- 10000+块: Pinecone、Qdrant、Milvus

### Q7: 如何调试RAG效果？

**方法1**: 使用纯检索API查看检索结果

```bash
curl -X POST "http://localhost:8080/api/rag/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "你的查询",
    "topK": 5,
    "similarityThreshold": 0.6
  }'
```

检查`documents`字段，确认检索到的内容是否相关。

**方法2**: 启用DEBUG日志

```yaml
# application.yaml
logging:
  level:
    com.lxq.spring_api_chat.rag: DEBUG
```

**方法3**: 使用Spring AI的日志输出

```java
QuestionAnswerAdvisor.builder()
    // ... 配置
    .logAdvisedText(true)  // 启用日志输出
    .build();
```

---

## 7. 故障排查

### 7.1 常见错误

#### 错误1: `文件不存在`

```
错误: 文件不存在: E:\AI\spring-ai-chat\data\documents\your-doc.md
```

**原因**: 文件路径错误

**解决**:
```bash
# 检查文件是否存在
ls data/documents/

# 使用正确的文件名
curl -X POST "http://localhost:8080/api/rag/index?filePath=正确的文件名.md"
```

#### 错误2: `topK必须在1-50之间`

```
错误: topK必须在1-50之间
```

**原因**: topK参数超出范围

**解决**: 使用有效范围的topK（1-50）

```bash
curl -X POST "http://localhost:8080/api/chat/rag?topK=5"
```

#### 错误3: `访问被拒绝：文件路径不安全`

```
访问被拒绝：文件路径不安全
```

**原因**: 试图访问`data/`目录之外的文件（安全保护）

**解决**: 将文档放到`data/documents`目录下

#### 错误4: 索引PDF失败

```
索引失败: Failed to read PDF
```

**可能原因**:
1. PDF损坏或加密
2. PDF格式不兼容

**解决**:
1. 尝试重新生成PDF
2. 使用PDF转Markdown工具转换后索引

### 7.2 性能问题

#### 问题1: 索引速度慢

**现象**: 索引大型PDF需要很长时间

**原因**:
- 文档过大
- Embedding API调用慢
- ChunkSize过小，导致过多的API调用

**解决**:
1. 增大ChunkSize（减少块数量）
2. 批量处理，使用异步索引
3. 检查网络延迟

#### 问题2: 查询响应慢

**现象**: RAG查询需要5秒以上

**原因**:
- 检索慢（文档块过多）
- LLM响应慢
- topK太大

**解决**:
1. 升级VectorStore
2. 减少topK
3. 使用缓存

### 7.3 日志分析

启用DEBUG日志查看详细信息：

```yaml
# application-openai.yaml
logging:
  level:
    root: INFO
    com.lxq.spring_api_chat: DEBUG
    org.springframework.ai: DEBUG
```

**关键日志**:

```
✓ 成功加载向量存储: data/vectorstore/simple-vector-store.json
✓ 向量存储已持久化: data/vectorstore/simple-vector-store.json
ChatService.chatWithRag - 使用会话ID: xxx, 消息: xxx
```

### 7.4 获取帮助

如果问题仍未解决，请：

1. **查看日志**: 检查应用日志输出
2. **查看文档**: 参考`doc/11-RAG技术指南.md`
3. **提交Issue**: 在项目GitHub仓库提交Issue，包含：
   - 错误日志
   - 请求示例
   - 环境信息（JDK版本、OS等）

---

## 附录

### A. API端点总览

| 端点 | 方法 | 功能 | 参数 |
|------|------|------|------|
| `/api/rag/index` | POST | 索引单个文档 | filePath |
| `/api/rag/index-directory` | POST | 批量索引目录 | directoryPath |
| `/api/rag/query` | POST | 纯检索 | QueryRequest |
| `/api/rag/stats` | GET | 获取统计信息 | 无 |
| `/api/chat/rag` | POST | RAG增强对话 | ChatRequest, topK, threshold |
| `/api/chat/rag-stream` | POST | RAG流式对话 | ChatRequest, topK, threshold |

### B. 配置参数参考

```yaml
# application-openai.yaml

spring:
  ai:
    openai:
      embedding:
        options:
          model: text-embedding-3-small

rag:
  vectorstore:
    path: data/vectorstore/simple-vector-store.json
  chunking:
    size: 500
    overlap: 100
  retrieval:
    topK: 5
    similarityThreshold: 0.7
```

### C. 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| 检索增强生成 | RAG | Retrieval-Augmented Generation |
| 向量化 | Embedding | 将文本转换为向量表示 |
| 向量存储 | VectorStore | 存储和检索向量的数据库 |
| 相似度检索 | Similarity Search | 基于向量相似度的搜索 |
| 文档分块 | Chunking | 将大文档切分为小块 |
| 上下文窗口 | Context Window | LLM一次能处理的最大token数 |

---

**文档维护者**: Spring AI Chat 项目组
**最后更新**: 2025-12-22
**反馈渠道**: GitHub Issues
