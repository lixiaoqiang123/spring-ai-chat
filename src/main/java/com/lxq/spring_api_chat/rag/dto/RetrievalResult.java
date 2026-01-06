package com.lxq.spring_api_chat.rag.dto;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档检索结果
 */
public record RetrievalResult(
    String query,             // 查询文本
    List<Document> documents, // 检索到的文档
    int count,                // 文档数量
    long duration,            // 耗时(ms)
    boolean success,          // 是否成功
    String errorMessage       // 错误信息
) {}
