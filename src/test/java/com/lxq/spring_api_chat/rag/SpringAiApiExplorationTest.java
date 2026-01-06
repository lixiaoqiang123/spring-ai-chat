package com.lxq.spring_api_chat.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * Spring AI API探索测试
 * 用于验证Spring AI 1.1.0的实际API
 */
@SpringBootTest
public class SpringAiApiExplorationTest {

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Test
    public void testDocumentApi() {
        // 测试Document类的API
        Document doc = new Document("测试内容");
        System.out.println("Document创建成功");

        // 尝试获取内容
        try {
            // 方法1: getText() - Spring AI 1.1.0使用这个
            String content = doc.getText();
            System.out.println("✓ Document.getText() 可用: " + content);
        } catch (Exception e) {
            System.out.println("✗ Document.getText() 不可用: " + e.getMessage());
        }

        // 测试元数据
        Map<String, Object> metadata = doc.getMetadata();
        System.out.println("✓ Document.getMetadata() 可用: " + metadata);
    }

    @Test
    public void testEmbeddingModel() {
        if (embeddingModel != null) {
            System.out.println("✓ EmbeddingModel 已注入: " + embeddingModel.getClass().getName());

            // 测试embedding
            try {
                var result = embeddingModel.embed("测试文本");
                System.out.println("✓ EmbeddingModel.embed() 可用,维度: " + result.length);
            } catch (Exception e) {
                System.out.println("✗ EmbeddingModel.embed() 失败: " + e.getMessage());
            }
        } else {
            System.out.println("✗ EmbeddingModel 未注入(可能需要配置)");
        }
    }

    @Test
    public void testTextSplitter() {
        // 测试TextSplitter
        System.out.println("测试TextSplitter API...");

        // 尝试不同的构造方式
        try {
            // 方式1: 无参构造
            TextSplitter splitter1 = new org.springframework.ai.transformer.splitter.TokenTextSplitter();
            System.out.println("✓ TokenTextSplitter() 无参构造可用");
        } catch (Exception e) {
            System.out.println("✗ TokenTextSplitter() 无参构造不可用: " + e.getMessage());
        }

        try {
            // 方式2: builder方式构造
            TextSplitter splitter2 = org.springframework.ai.transformer.splitter.TokenTextSplitter.builder()
                .withChunkSize(500)
                .withMinChunkSizeChars(100)
                .build();
            System.out.println("✓ TokenTextSplitter.builder() 构造可用");
        } catch (Exception e) {
            System.out.println("✗ TokenTextSplitter.builder() 构造不可用: " + e.getMessage());
        }
    }

    @Test
    public void testVectorStoreClasses() {
        System.out.println("探索VectorStore相关类...");

        // 尝试导入VectorStore
        try {
            Class<?> vectorStoreClass = Class.forName("org.springframework.ai.vectorstore.VectorStore");
            System.out.println("✓ VectorStore 类存在: " + vectorStoreClass.getName());

            // 列出方法
            System.out.println("VectorStore 方法:");
            for (var method : vectorStoreClass.getMethods()) {
                if (method.getDeclaringClass() == vectorStoreClass) {
                    System.out.println("  - " + method.getName() + "()");
                }
            }
        } catch (ClassNotFoundException e) {
            System.out.println("✗ VectorStore 类不存在");
        }

        // 尝试导入SimpleVectorStore
        try {
            Class<?> simpleVectorStoreClass = Class.forName("org.springframework.ai.vectorstore.SimpleVectorStore");
            System.out.println("✓ SimpleVectorStore 类存在: " + simpleVectorStoreClass.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("✗ SimpleVectorStore 类不存在");
        }

        // 尝试导入SearchRequest
        try {
            Class<?> searchRequestClass = Class.forName("org.springframework.ai.vectorstore.SearchRequest");
            System.out.println("✓ SearchRequest 类存在: " + searchRequestClass.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("✗ SearchRequest 类不存在");
        }
    }

    @Test
    public void exploreAvailableClasses() {
        System.out.println("探索spring-ai包中的可用类...");

        String[] classesToCheck = {
            "org.springframework.ai.vectorstore.VectorStore",
            "org.springframework.ai.vectorstore.SimpleVectorStore",
            "org.springframework.ai.vectorstore.InMemoryVectorStore",
            "org.springframework.ai.vectorstore.SearchRequest",
            "org.springframework.ai.vectorstore.filter.SearchRequest",
            "org.springframework.ai.document.Document",
            "org.springframework.ai.embedding.EmbeddingModel",
            "org.springframework.ai.transformer.splitter.TokenTextSplitter",
            "org.springframework.ai.transformer.splitter.TextSplitter"
        };

        for (String className : classesToCheck) {
            try {
                Class<?> clazz = Class.forName(className);
                System.out.println("✓ " + className + " 存在");
            } catch (ClassNotFoundException e) {
                System.out.println("✗ " + className + " 不存在");
            }
        }
    }
}
