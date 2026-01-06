package com.lxq.spring_api_chat.rag.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RAG配置类
 * 配置向量存储、文本分块器等核心组件
 */
@Configuration
public class RagConfig {

    @Value("${rag.vectorstore.path:data/vectorstore/simple-vector-store.json}")
    private String vectorStorePath;

    @Value("${rag.chunking.size:500}")
    private int chunkSize;

    @Value("${rag.chunking.overlap:100}")
    private int chunkOverlap;

    /**
     * 配置SimpleVectorStore
     * 使用内存存储,支持持久化到JSON文件
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 尝试从文件加载已有的向量数据
        File storeFile = new File(vectorStorePath);
        if (storeFile.exists()) {
            try {
                vectorStore.load(storeFile);
                System.out.println("✓ 成功加载向量存储: " + vectorStorePath);
            } catch (Exception e) {
                System.err.println("✗ 加载向量存储失败: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ 向量存储文件不存在,将创建新的存储: " + vectorStorePath);
            // 确保目录存在
            storeFile.getParentFile().mkdirs();
        }

        return vectorStore;
    }

    /**
     * 配置文本分块器
     * 使用TokenTextSplitter进行智能分块
     */
    @Bean
    public TokenTextSplitter textSplitter() {
        return TokenTextSplitter.builder()
            .withChunkSize(chunkSize)           // 每块500个token
            .withMinChunkSizeChars(chunkOverlap) // 最小字符数
            .build();
    }

    /**
     * 配置文档存储路径
     */
    @Bean
    public Path documentBasePath() {
        return Paths.get("data/documents");
    }
}
