# Spring AI 技术指南

## Spring AI 简介

Spring AI 是 Spring 团队推出的用于集成大语言模型（LLM）的框架。

## 核心概念

### ChatClient

ChatClient 是 Spring AI 的核心接口，用于与大语言模型进行交互。

### Advisor

Advisor 是 Spring AI 的增强组件，可以在请求前后添加额外的处理逻辑。

常用的 Advisor 包括：
- MessageChatMemoryAdvisor - 管理对话历史
- QuestionAnswerAdvisor - 实现 RAG 功能

### VectorStore

VectorStore 用于存储文档的向量表示，支持相似度检索。

## RAG 实现

RAG（Retrieval-Augmented Generation）通过检索相关文档来增强生成效果。

实现步骤：
1. 文档索引 - 将文档转换为向量并存储
2. 相似度检索 - 根据查询检索最相关的文档
3. 增强生成 - 将检索到的文档作为上下文生成答案

### 文档索引

使用 DocumentIndexingService 可以索引 PDF、Markdown、TXT 等格式的文档。

### 相似度检索

使用 VectorStore 的 similaritySearch 方法可以检索最相关的文档块。

参数说明：
- query: 查询文本
- topK: 返回前 K 个最相关的结果
- similarityThreshold: 相似度阈值，过滤低相关性的结果

### QuestionAnswerAdvisor

QuestionAnswerAdvisor 自动将检索到的文档注入到 Prompt 上下文中。

配置示例：
```java
QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder()
    .vectorStore(vectorStore)
    .searchRequest(SearchRequest.builder()
        .query(query)
        .topK(5)
        .similarityThreshold(0.7)
        .build())
    .build();
```

## 最佳实践

1. 合理设置 chunkSize，平衡上下文完整性和检索精度
2. 使用适当的 similarityThreshold 过滤低质量结果
3. 结合 Memory 和 RAG 实现智能对话
4. 定期持久化 VectorStore
