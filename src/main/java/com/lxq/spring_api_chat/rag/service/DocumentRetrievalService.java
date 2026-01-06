package com.lxq.spring_api_chat.rag.service;

import com.lxq.spring_api_chat.rag.dto.RetrievalResult;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档检索服务
 * 负责向量相似度检索
 */
@Service
public class DocumentRetrievalService {

    private final VectorStore vectorStore;

    @Value("${rag.retrieval.topK:5}")
    private int defaultTopK;

    @Value("${rag.retrieval.similarityThreshold:0.7}")
    private double defaultSimilarityThreshold;

    public DocumentRetrievalService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 检索相关文档
     * @param query 查询文本
     * @return 检索结果
     */
    public RetrievalResult retrieve(String query) {
        return retrieve(query, defaultTopK, defaultSimilarityThreshold);
    }

    /**
     * 检索相关文档(自定义参数)
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值
     * @return 检索结果
     */
    public RetrievalResult retrieve(String query, int topK, double similarityThreshold) {
        long startTime = System.currentTimeMillis();

        try {
            // 执行向量检索
            List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build()
            );

            long duration = System.currentTimeMillis() - startTime;

            return new RetrievalResult(
                query,
                documents,
                documents.size(),
                duration,
                true,
                null
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new RetrievalResult(
                query,
                List.of(),
                0,
                duration,
                false,
                e.getMessage()
            );
        }
    }

    /**
     * 格式化检索结果为上下文字符串
     */
    public String formatContext(List<Document> documents) {
        return documents.stream()
            .map(doc -> {
                String source = doc.getMetadata().getOrDefault("source", "未知来源").toString();
                return String.format("[来源: %s]\n%s", source, doc.getText());
            })
            .collect(Collectors.joining("\n\n---\n\n"));
    }
}
