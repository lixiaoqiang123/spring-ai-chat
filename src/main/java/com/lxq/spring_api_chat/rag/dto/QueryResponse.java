package com.lxq.spring_api_chat.rag.dto;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * RAG查询响应
 *
 * @param query 原始查询文本
 * @param documents 检索到的相关文档列表
 * @param documentCount 文档数量
 * @param context 格式化后的上下文文本（所有文档内容拼接）
 */
public record QueryResponse(
    String query,
    List<DocumentInfo> documents,
    int documentCount,
    String context
) {
    /**
     * 文档信息
     *
     * @param content 文档内容
     * @param source 文档来源
     * @param score 相似度分数
     * @param metadata 元数据
     */
    public record DocumentInfo(
        String content,
        String source,
        Double score,
        java.util.Map<String, Object> metadata
    ) {
        /**
         * 从Spring AI的Document转换
         */
        public static DocumentInfo from(Document document) {
            return new DocumentInfo(
                document.getText(),
                (String) document.getMetadata().get("source"),
                (Double) document.getMetadata().get("distance"),
                document.getMetadata()
            );
        }
    }

    /**
     * 创建查询响应的工厂方法
     */
    public static QueryResponse from(String query, List<Document> documents) {
        List<DocumentInfo> docInfos = documents.stream()
            .map(DocumentInfo::from)
            .toList();

        String context = documents.stream()
            .map(Document::getText)
            .reduce("", (a, b) -> a + "\n\n" + b)
            .trim();

        return new QueryResponse(
            query,
            docInfos,
            documents.size(),
            context
        );
    }
}
