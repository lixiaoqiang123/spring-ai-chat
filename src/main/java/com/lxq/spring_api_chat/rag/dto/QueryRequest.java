package com.lxq.spring_api_chat.rag.dto;

/**
 * RAG查询请求
 *
 * @param query 用户查询文本
 * @param topK 返回最相关的K个文档块，默认5
 * @param similarityThreshold 相似度阈值，0-1之间，默认0.7
 */
public record QueryRequest(
    String query,
    Integer topK,
    Double similarityThreshold
) {
    /**
     * 构造器，提供默认值
     */
    public QueryRequest {
        if (topK == null) {
            topK = 5;
        }
        if (similarityThreshold == null) {
            similarityThreshold = 0.7;
        }

        // 参数验证
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }
        if (topK <= 0 || topK > 50) {
            throw new IllegalArgumentException("topK必须在1-50之间");
        }
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException("相似度阈值必须在0.0-1.0之间");
        }
    }

    /**
     * 简化构造器 - 只提供查询文本，使用默认参数
     */
    public QueryRequest(String query) {
        this(query, 5, 0.7);
    }
}
