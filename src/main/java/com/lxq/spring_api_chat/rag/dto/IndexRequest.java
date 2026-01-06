package com.lxq.spring_api_chat.rag.dto;

/**
 * 文档索引请求
 */
public record IndexRequest(
    String filePath,      // 文件路径
    String docType        // 文档类型: PDF, MARKDOWN, TEXT
) {
    public IndexRequest {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
    }
}
