package com.lxq.spring_api_chat.rag.dto;

/**
 * 文档索引响应
 */
public record IndexResponse(
    String filename,          // 文件名
    int documentCount,        // 原始文档数
    int chunkCount,           // 分块数
    long duration,            // 耗时(ms)
    boolean success,          // 是否成功
    String errorMessage       // 错误信息
) {
    public static IndexResponse success(
        String filename,
        int documentCount,
        int chunkCount,
        long duration
    ) {
        return new IndexResponse(
            filename,
            documentCount,
            chunkCount,
            duration,
            true,
            null
        );
    }

    public static IndexResponse failure(
        String filename,
        String errorMessage,
        long duration
    ) {
        return new IndexResponse(
            filename,
            0,
            0,
            duration,
            false,
            errorMessage
        );
    }
}
