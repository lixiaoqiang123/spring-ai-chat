package com.lxq.spring_api_chat.rag.controller;

import com.lxq.spring_api_chat.rag.dto.IndexResponse;
import com.lxq.spring_api_chat.rag.dto.QueryRequest;
import com.lxq.spring_api_chat.rag.dto.QueryResponse;
import com.lxq.spring_api_chat.rag.dto.StatsResponse;
import com.lxq.spring_api_chat.rag.service.DocumentIndexingService;
import com.lxq.spring_api_chat.rag.service.DocumentRetrievalService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * RAG功能REST API控制器
 * 提供文档索引、查询、统计等功能
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentIndexingService indexingService;
    private final DocumentRetrievalService retrievalService;
    private final VectorStore vectorStore;

    @Value("${rag.vectorstore.path:data/vectorstore/simple-vector-store.json}")
    private String vectorStorePath;

    @Value("${rag.chunking.size:500}")
    private int chunkSize;

    @Value("${rag.chunking.overlap:100}")
    private int chunkOverlap;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String embeddingModel;

    public RagController(
        DocumentIndexingService indexingService,
        DocumentRetrievalService retrievalService,
        VectorStore vectorStore
    ) {
        this.indexingService = indexingService;
        this.retrievalService = retrievalService;
        this.vectorStore = vectorStore;
    }

    /**
     * 索引单个文档
     *
     * @param filePath 文档路径（支持绝对路径或相对于data/documents的相对路径）
     * @return 索引结果
     */
    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexDocument(@RequestParam String filePath) {
        try {
            // 验证和解析文件路径
            File file = resolveFilePath(filePath);

            if (!file.exists()) {
                return ResponseEntity.badRequest()
                    .body(IndexResponse.failure(
                        filePath,
                        "文件不存在: " + file.getAbsolutePath(),
                        0
                    ));
            }

            if (!file.isFile()) {
                return ResponseEntity.badRequest()
                    .body(IndexResponse.failure(
                        filePath,
                        "不是有效的文件: " + file.getAbsolutePath(),
                        0
                    ));
            }

            // 安全检查：防止路径遍历攻击
            if (!isPathSafe(file.toPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(IndexResponse.failure(
                        filePath,
                        "访问被拒绝：文件路径不安全",
                        0
                    ));
            }

            // 执行索引
            Resource resource = new FileSystemResource(file);
            IndexResponse response = indexingService.indexDocument(resource);

            return response.success()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(IndexResponse.failure(
                    filePath,
                    "索引失败: " + e.getMessage(),
                    0
                ));
        }
    }

    /**
     * 批量索引目录下的所有文档
     *
     * @param directoryPath 目录路径（支持绝对路径或相对于data/documents的相对路径）
     * @return 索引结果列表
     */
    @PostMapping("/index-directory")
    public ResponseEntity<List<IndexResponse>> indexDirectory(@RequestParam String directoryPath) {
        try {
            // 验证和解析目录路径
            File directory = resolveFilePath(directoryPath);

            if (!directory.exists()) {
                return ResponseEntity.badRequest()
                    .body(List.of(IndexResponse.failure(
                        directoryPath,
                        "目录不存在: " + directory.getAbsolutePath(),
                        0
                    )));
            }

            if (!directory.isDirectory()) {
                return ResponseEntity.badRequest()
                    .body(List.of(IndexResponse.failure(
                        directoryPath,
                        "不是有效的目录: " + directory.getAbsolutePath(),
                        0
                    )));
            }

            // 安全检查
            if (!isPathSafe(directory.toPath())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(List.of(IndexResponse.failure(
                        directoryPath,
                        "访问被拒绝：目录路径不安全",
                        0
                    )));
            }

            // 执行批量索引
            List<IndexResponse> responses = indexingService.indexDirectory(directory.toPath());

            return ResponseEntity.ok(responses);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(List.of(IndexResponse.failure(
                    directoryPath,
                    "批量索引失败: " + e.getMessage(),
                    0
                )));
        }
    }

    /**
     * RAG查询 - 检索相关文档
     *
     * @param request 查询请求
     * @return 查询响应，包含检索到的文档
     */
    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        try {
            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                .query(request.query())
                .topK(request.topK())
                .similarityThreshold(request.similarityThreshold())
                .build();

            // 执行相似度检索
            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            // 构建响应
            QueryResponse response = QueryResponse.from(request.query(), documents);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
        }
    }

    /**
     * 获取RAG系统统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        try {
            // 获取向量存储大小
            // 注意：SimpleVectorStore没有直接的size()方法，需要通过查询来估算
            // 这里使用一个技巧：执行一个空查询并设置很大的topK
            List<Document> allDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query("")
                    .topK(10000)
                    .similarityThreshold(0.0)
                    .build()
            );

            int vectorStoreSize = allDocs.size();

            // 构建统计响应
            StatsResponse response = StatsResponse.of(
                vectorStoreSize,
                vectorStorePath,
                embeddingModel,
                chunkSize,
                chunkOverlap
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 如果获取失败，返回默认值
            StatsResponse response = StatsResponse.of(
                0,
                vectorStorePath,
                embeddingModel,
                chunkSize,
                chunkOverlap
            );
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 解析文件路径
     * 支持绝对路径和相对路径（相对于data/documents）
     */
    private File resolveFilePath(String filePath) {
        File file = new File(filePath);

        // 如果是绝对路径，直接使用
        if (file.isAbsolute()) {
            return file;
        }

        // 否则，相对于data/documents目录
        Path basePath = Paths.get("data/documents");
        return basePath.resolve(filePath).toFile();
    }

    /**
     * 安全检查：防止路径遍历攻击
     * 确保访问的文件在允许的目录范围内
     */
    private boolean isPathSafe(Path path) {
        try {
            // 获取规范化的绝对路径
            Path normalizedPath = path.toAbsolutePath().normalize();

            // 允许的基础目录
            Path dataDir = Paths.get("data").toAbsolutePath().normalize();
            Path currentDir = Paths.get(".").toAbsolutePath().normalize();

            // 检查路径是否在允许的目录下
            return normalizedPath.startsWith(dataDir) ||
                   normalizedPath.startsWith(currentDir);

        } catch (Exception e) {
            return false;
        }
    }
}
