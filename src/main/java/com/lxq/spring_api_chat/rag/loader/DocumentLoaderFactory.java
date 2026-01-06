package com.lxq.spring_api_chat.rag.loader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档加载器工厂
 * 根据文件类型选择合适的加载器
 */
@Component
public class DocumentLoaderFactory {

    /**
     * 加载文档
     * @param resource 文档资源
     * @return 文档列表
     */
    public List<Document> loadDocument(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            throw new IllegalArgumentException("无法获取文件名");
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return loadPdfDocument(resource);
        } else if (filename.toLowerCase().endsWith(".md") ||
                   filename.toLowerCase().endsWith(".txt")) {
            return loadTextDocument(resource);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + filename);
        }
    }

    /**
     * 加载PDF文档
     */
    private List<Document> loadPdfDocument(Resource resource) {
        var config = PdfDocumentReaderConfig.builder()
            .withPageTopMargin(0)
            .withPageBottomMargin(0)
            .withPageExtractedTextFormatter(
                ExtractedTextFormatter.builder()
                    .withNumberOfTopTextLinesToDelete(0)
                    .build()
            )
            .build();

        var reader = new PagePdfDocumentReader(resource, config);
        return reader.get();
    }

    /**
     * 加载文本文档(Markdown/TXT)
     */
    private List<Document> loadTextDocument(Resource resource) {
        var reader = new TextReader(resource);
        return reader.get();
    }
}
