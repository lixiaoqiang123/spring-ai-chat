package com.lxq.spring_api_chat.rag.service;

import com.lxq.spring_api_chat.rag.dto.IndexResponse;
import com.lxq.spring_api_chat.rag.loader.DocumentLoaderFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 文档索引服务
 * 负责加载、分块、向量化和存储文档
 */
@Service
public class DocumentIndexingService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final DocumentLoaderFactory loaderFactory;

    @Value("${rag.vectorstore.path:data/vectorstore/simple-vector-store.json}")
    private String vectorStorePath;

    public DocumentIndexingService(
        VectorStore vectorStore,
        TokenTextSplitter textSplitter,
        DocumentLoaderFactory loaderFactory
    ) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.loaderFactory = loaderFactory;
    }

    /**
     * 索引单个文档
     */
    public IndexResponse indexDocument(Resource resource) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 加载文档
            List<Document> documents = loaderFactory.loadDocument(resource);

            // 2. 分块
            List<Document> chunks = textSplitter.apply(documents);

            // 3. 添加元数据
            String filename = resource.getFilename();
            for (Document chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("source", filename);
                metadata.put("indexedAt", LocalDateTime.now().toString());
                metadata.put("docType", getDocType(filename));
                chunk.getMetadata().putAll(metadata);
            }

            // 4. 向量化并存储
            vectorStore.add(chunks);

            // 5. 持久化到JSON文件
            persistVectorStore();

            long duration = System.currentTimeMillis() - startTime;
            return IndexResponse.success(
                filename,
                documents.size(),
                chunks.size(),
                duration
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return IndexResponse.failure(
                resource.getFilename(),
                e.getMessage(),
                duration
            );
        }
    }

    /**
     * 批量索引目录下的所有文档
     */
    public List<IndexResponse> indexDirectory(Path directory) {
        List<IndexResponse> results = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isSupportedFile)
                 .forEach(path -> {
                     Resource resource = new FileSystemResource(path);
                     IndexResponse response = indexDocument(resource);
                     results.add(response);
                 });
        } catch (IOException e) {
            System.err.println("遍历目录失败: " + e.getMessage());
        }

        return results;
    }

    /**
     * 持久化向量存储到JSON文件
     */
    public void persistVectorStore() {
        try {
            File storeFile = new File(vectorStorePath);
            storeFile.getParentFile().mkdirs();

            if (vectorStore instanceof SimpleVectorStore simpleStore) {
                simpleStore.save(storeFile);
                System.out.println("✓ 向量存储已持久化: " + vectorStorePath);
            }
        } catch (Exception e) {
            System.err.println("✗ 持久化向量存储失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为支持的文件类型
     */
    private boolean isSupportedFile(Path path) {
        String filename = path.getFileName().toString().toLowerCase();
        return filename.endsWith(".pdf") ||
               filename.endsWith(".md") ||
               filename.endsWith(".txt");
    }

    /**
     * 获取文档类型
     */
    private String getDocType(String filename) {
        if (filename.endsWith(".pdf")) return "PDF";
        if (filename.endsWith(".md")) return "MARKDOWN";
        if (filename.endsWith(".txt")) return "TEXT";
        return "UNKNOWN";
    }
}
