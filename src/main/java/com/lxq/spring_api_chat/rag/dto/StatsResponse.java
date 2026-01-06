package com.lxq.spring_api_chat.rag.dto;

import java.time.LocalDateTime;

/**
 * RAG统计信息响应
 *
 * @param vectorStoreSize 向量存储中的文档块数量
 * @param vectorStorePath 向量存储路径
 * @param embeddingModel 使用的Embedding模型
 * @param chunkSize 文档分块大小
 * @param chunkOverlap 分块重叠大小
 * @param timestamp 统计时间戳
 */
public record StatsResponse(
    int vectorStoreSize,
    String vectorStorePath,
    String embeddingModel,
    int chunkSize,
    int chunkOverlap,
    LocalDateTime timestamp
) {
    /**
     * 创建统计响应的工厂方法
     */
    public static StatsResponse of(
        int vectorStoreSize,
        String vectorStorePath,
        String embeddingModel,
        int chunkSize,
        int chunkOverlap
    ) {
        return new StatsResponse(
            vectorStoreSize,
            vectorStorePath,
            embeddingModel,
            chunkSize,
            chunkOverlap,
            LocalDateTime.now()
        );
    }
}
