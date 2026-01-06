package com.lxq.spring_api_chat.rag;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import com.lxq.spring_api_chat.chat.dto.ChatResponse;
import com.lxq.spring_api_chat.chat.service.ChatService;
import com.lxq.spring_api_chat.rag.dto.IndexResponse;
import com.lxq.spring_api_chat.rag.dto.QueryRequest;
import com.lxq.spring_api_chat.rag.dto.QueryResponse;
import com.lxq.spring_api_chat.rag.service.DocumentIndexingService;
import org.junit.jupiter.api.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RAG功能集成测试
 * 测试文档索引、检索、RAG对话的完整流程
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RagIntegrationTest {

    @Autowired
    private DocumentIndexingService indexingService;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatService chatService;

    private static final String TEST_DOCS_PATH = "test-documents";

    /**
     * 测试1: 索引单个文档
     */
    @Test
    @Order(1)
    @DisplayName("测试索引单个Markdown文档")
    public void testIndexSingleDocument() throws Exception {
        // 准备测试文档
        Resource resource = new ClassPathResource(TEST_DOCS_PATH + "/spring-ai-guide.md");
        assertTrue(resource.exists(), "测试文档应该存在");

        // 执行索引
        IndexResponse response = indexingService.indexDocument(resource);

        // 验证索引结果
        assertTrue(response.success(), "索引应该成功");
        assertNotNull(response.filename(), "文件名不应为空");
        assertTrue(response.chunkCount() > 0, "应该创建文档块");
        assertTrue(response.duration() >= 0, "执行时间应该非负");

        System.out.println("索引结果: " + response);
    }

    /**
     * 测试2: 批量索引目录
     */
    @Test
    @Order(2)
    @DisplayName("测试批量索引目录")
    public void testIndexDirectory() throws Exception {
        // 获取测试文档目录
        Resource dirResource = new ClassPathResource(TEST_DOCS_PATH);
        File testDocsDir = dirResource.getFile();
        assertTrue(testDocsDir.isDirectory(), "应该是目录");

        // 执行批量索引
        Path dirPath = testDocsDir.toPath();
        List<IndexResponse> responses = indexingService.indexDirectory(dirPath);

        // 验证批量索引结果
        assertFalse(responses.isEmpty(), "应该有索引结果");
        long successCount = responses.stream().filter(IndexResponse::success).count();
        assertTrue(successCount > 0, "应该有成功的索引");

        System.out.println("批量索引结果: ");
        responses.forEach(response ->
            System.out.println("  - " + response.filename() + ": " +
                             (response.success() ? "成功" : "失败"))
        );
    }

    /**
     * 测试3: 相似度检索 - 精确匹配
     */
    @Test
    @Order(3)
    @DisplayName("测试相似度检索 - Spring AI查询")
    public void testSimilaritySearch_SpringAI() {
        // 执行检索
        String query = "什么是Spring AI的QuestionAnswerAdvisor？";
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.5)
                .build()
        );

        // 验证检索结果
        assertFalse(documents.isEmpty(), "应该检索到相关文档");
        assertTrue(documents.size() <= 3, "结果数量不应超过topK");

        System.out.println("\n查询: " + query);
        System.out.println("检索到 " + documents.size() + " 个相关文档:");
        documents.forEach(doc -> {
            System.out.println("\n---");
            System.out.println("来源: " + doc.getMetadata().get("source"));
            System.out.println("内容片段: " + doc.getText().substring(0,
                Math.min(100, doc.getText().length())) + "...");
            System.out.println("相似度分数: " + doc.getMetadata().get("distance"));
        });
    }

    /**
     * 测试4: 相似度检索 - 模糊匹配
     */
    @Test
    @Order(4)
    @DisplayName("测试相似度检索 - Java 21查询")
    public void testSimilaritySearch_Java21() {
        // 执行检索
        String query = "Java 21有哪些新特性？";
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.5)
                .build()
        );

        // 验证检索结果
        assertFalse(documents.isEmpty(), "应该检索到相关文档");

        System.out.println("\n查询: " + query);
        System.out.println("检索到 " + documents.size() + " 个相关文档:");
        documents.forEach(doc -> {
            System.out.println("\n---");
            System.out.println("来源: " + doc.getMetadata().get("source"));
            System.out.println("内容片段: " + doc.getText().substring(0,
                Math.min(150, doc.getText().length())) + "...");
        });
    }

    /**
     * 测试5: TopK参数验证
     */
    @Test
    @Order(5)
    @DisplayName("测试TopK参数控制结果数量")
    public void testTopKParameter() {
        String query = "Spring AI";

        // 测试 topK=1
        List<Document> docs1 = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(1)
                .similarityThreshold(0.3)
                .build()
        );
        assertTrue(docs1.size() <= 1, "topK=1应该最多返回1个结果");

        // 测试 topK=3
        List<Document> docs3 = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.3)
                .build()
        );
        assertTrue(docs3.size() <= 3, "topK=3应该最多返回3个结果");

        System.out.println("topK=1: " + docs1.size() + " 个结果");
        System.out.println("topK=3: " + docs3.size() + " 个结果");
    }

    /**
     * 测试6: 相似度阈值过滤
     */
    @Test
    @Order(6)
    @DisplayName("测试相似度阈值过滤")
    public void testSimilarityThreshold() {
        String query = "Spring AI";

        // 低阈值
        List<Document> docsLowThreshold = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.3)
                .build()
        );

        // 高阈值
        List<Document> docsHighThreshold = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(10)
                .similarityThreshold(0.8)
                .build()
        );

        // 高阈值的结果应该不多于低阈值
        assertTrue(docsHighThreshold.size() <= docsLowThreshold.size(),
            "高阈值应该过滤掉更多结果");

        System.out.println("阈值0.3: " + docsLowThreshold.size() + " 个结果");
        System.out.println("阈值0.8: " + docsHighThreshold.size() + " 个结果");
    }

    /**
     * 测试7: RAG对话 - 基于文档内容回答
     */
    @Test
    @Order(7)
    @DisplayName("测试RAG增强对话")
    public void testRagChat() {
        // 准备请求
        ChatRequest request = new ChatRequest(
            "请介绍一下Spring AI的QuestionAnswerAdvisor是什么？",
            null
        );

        // 执行RAG对话
        ChatResponse response = chatService.chatWithRag(request, 5, 0.6);

        // 验证响应
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getReply(), "回复内容不应为空");
        assertNotNull(response.getSessionId(), "会话ID不应为空");
        assertFalse(response.getReply().isEmpty(), "回复内容不应为空字符串");

        System.out.println("\n=== RAG对话测试 ===");
        System.out.println("问题: " + request.message());
        System.out.println("回答: " + response.getReply());
        System.out.println("会话ID: " + response.getSessionId());
    }

    /**
     * 测试8: RAG对话 - 检索为空的降级处理
     */
    @Test
    @Order(8)
    @DisplayName("测试RAG对话 - 无相关文档时的处理")
    public void testRagChat_NoRelevantDocuments() {
        // 准备一个与文档完全无关的查询
        ChatRequest request = new ChatRequest(
            "今天北京的天气怎么样？",
            null
        );

        // 执行RAG对话（高阈值，确保检索不到文档）
        ChatResponse response = chatService.chatWithRag(request, 5, 0.95);

        // 验证响应
        assertNotNull(response, "响应不应为空");
        assertNotNull(response.getReply(), "回复内容不应为空");

        System.out.println("\n=== 无相关文档测试 ===");
        System.out.println("问题: " + request.message());
        System.out.println("回答: " + response.getReply());
    }

    /**
     * 测试9: 持久化验证
     */
    @Test
    @Order(9)
    @DisplayName("测试向量存储持久化")
    public void testVectorStorePersistence() {
        // 执行持久化
        indexingService.persistVectorStore();

        // 验证持久化文件存在
        File storeFile = new File("data/vectorstore/simple-vector-store.json");
        assertTrue(storeFile.exists(), "持久化文件应该存在");
        assertTrue(storeFile.length() > 0, "持久化文件不应为空");

        System.out.println("持久化文件大小: " + storeFile.length() + " 字节");
    }

    /**
     * 测试10: 边界条件 - 空查询
     */
    @Test
    @Order(10)
    @DisplayName("测试边界条件 - 空查询处理")
    public void testEdgeCase_EmptyQuery() {
        // 测试空查询是否会抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("", 5, 0.7);
        }, "空查询应该抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("   ", 5, 0.7);
        }, "空白查询应该抛出异常");
    }

    /**
     * 测试11: 边界条件 - 无效参数
     */
    @Test
    @Order(11)
    @DisplayName("测试边界条件 - 无效参数")
    public void testEdgeCase_InvalidParameters() {
        // 测试无效的topK
        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("test", 0, 0.7);
        }, "topK=0应该抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("test", 100, 0.7);
        }, "topK=100应该抛出异常");

        // 测试无效的threshold
        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("test", 5, -0.1);
        }, "负阈值应该抛出异常");

        assertThrows(IllegalArgumentException.class, () -> {
            new QueryRequest("test", 5, 1.5);
        }, "阈值>1应该抛出异常");
    }

    /**
     * 清理测试数据
     */
    @AfterAll
    public static void cleanup() {
        System.out.println("\n=== RAG集成测试完成 ===");
    }
}
