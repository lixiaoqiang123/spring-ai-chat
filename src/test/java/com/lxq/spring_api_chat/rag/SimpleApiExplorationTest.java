package com.lxq.spring_api_chat.rag;

import org.junit.jupiter.api.Test;

/**
 * 简单的API探索测试
 * 不依赖Spring容器,直接探索类是否存在
 */
public class SimpleApiExplorationTest {

    @Test
    public void exploreDocumentApi() {
        System.out.println("=== 探索Document API ===");

        try {
            Class<?> docClass = Class.forName("org.springframework.ai.document.Document");
            System.out.println("✓ Document类存在");

            // 列出所有public方法
            System.out.println("\nDocument的public方法:");
            for (var method : docClass.getMethods()) {
                if (method.getDeclaringClass() == docClass) {
                    System.out.println("  - " + method.getName() + "(" +
                        java.util.Arrays.toString(method.getParameterTypes()) + ") : " +
                        method.getReturnType().getSimpleName());
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Document类探索失败: " + e.getMessage());
        }
    }

    @Test
    public void exploreVectorStoreApi() {
        System.out.println("\n=== 探索VectorStore API ===");

        String[] classesToCheck = {
            "org.springframework.ai.vectorstore.VectorStore",
            "org.springframework.ai.vectorstore.SimpleVectorStore",
            "org.springframework.ai.vectorstore.InMemoryVectorStore",
            "org.springframework.ai.vectorstore.SearchRequest",
            "org.springframework.ai.vectorstore.filter.SearchRequest"
        };

        for (String className : classesToCheck) {
            try {
                Class<?> clazz = Class.forName(className);
                System.out.println("✓ " + className + " 存在");

                // 如果是VectorStore,列出方法
                if (className.contains("VectorStore") && !className.contains("SearchRequest")) {
                    System.out.println("  方法:");
                    for (var method : clazz.getMethods()) {
                        if (method.getDeclaringClass() == clazz ||
                            method.getDeclaringClass().getName().contains("VectorStore")) {
                            System.out.println("    - " + method.getName());
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("✗ " + className + " 不存在");
            }
        }
    }

    @Test
    public void exploreTextSplitterApi() {
        System.out.println("\n=== 探索TextSplitter API ===");

        try {
            Class<?> splitterClass = Class.forName("org.springframework.ai.transformer.splitter.TokenTextSplitter");
            System.out.println("✓ TokenTextSplitter类存在");

            // 列出构造函数
            System.out.println("\nTokenTextSplitter的构造函数:");
            for (var constructor : splitterClass.getConstructors()) {
                System.out.print("  - TokenTextSplitter(");
                var params = constructor.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    System.out.print(params[i].getSimpleName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }
        } catch (Exception e) {
            System.out.println("✗ TokenTextSplitter探索失败: " + e.getMessage());
        }
    }

    @Test
    public void explorePdfReaderApi() {
        System.out.println("\n=== 探索PDF Reader API ===");

        try {
            Class<?> pdfReaderClass = Class.forName("org.springframework.ai.reader.pdf.PagePdfDocumentReader");
            System.out.println("✓ PagePdfDocumentReader类存在");

            // 列出构造函数
            System.out.println("\nPagePdfDocumentReader的构造函数:");
            for (var constructor : pdfReaderClass.getConstructors()) {
                System.out.print("  - PagePdfDocumentReader(");
                var params = constructor.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    System.out.print(params[i].getSimpleName());
                    if (i < params.length - 1) System.out.print(", ");
                }
                System.out.println(")");
            }

            // 检查PdfDocumentReaderConfig
            Class<?> configClass = Class.forName("org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig");
            System.out.println("✓ PdfDocumentReaderConfig类存在");

            // 查找ExtractedTextFormatter
            try {
                Class<?> formatterClass = Class.forName("org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig$ExtractedTextFormatter");
                System.out.println("✓ ExtractedTextFormatter内部类存在");
            } catch (ClassNotFoundException e) {
                System.out.println("✗ ExtractedTextFormatter内部类不存在");

                // 尝试其他可能的位置
                System.out.println("  尝试查找其他Formatter类...");
                for (Class<?> innerClass : configClass.getDeclaredClasses()) {
                    System.out.println("    - " + innerClass.getSimpleName());
                }
            }
        } catch (Exception e) {
            System.out.println("✗ PDF Reader探索失败: " + e.getMessage());
        }
    }

    @Test
    public void exploreAllSpringAiClasses() {
        System.out.println("\n=== 探索所有关键Spring AI类 ===");

        String[] allClasses = {
            // Document相关
            "org.springframework.ai.document.Document",
            "org.springframework.ai.document.DocumentReader",

            // Embedding相关
            "org.springframework.ai.embedding.EmbeddingModel",
            "org.springframework.ai.embedding.EmbeddingRequest",
            "org.springframework.ai.embedding.EmbeddingResponse",

            // VectorStore相关
            "org.springframework.ai.vectorstore.VectorStore",
            "org.springframework.ai.vectorstore.SimpleVectorStore",
            "org.springframework.ai.vectorstore.SearchRequest",

            // Transformer相关
            "org.springframework.ai.transformer.splitter.TextSplitter",
            "org.springframework.ai.transformer.splitter.TokenTextSplitter",

            // Reader相关
            "org.springframework.ai.reader.TextReader",
            "org.springframework.ai.reader.pdf.PagePdfDocumentReader",
            "org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig"
        };

        int existCount = 0;
        int notExistCount = 0;

        for (String className : allClasses) {
            try {
                Class.forName(className);
                System.out.println("✓ " + className);
                existCount++;
            } catch (ClassNotFoundException e) {
                System.out.println("✗ " + className);
                notExistCount++;
            }
        }

        System.out.println("\n统计: " + existCount + " 个类存在, " + notExistCount + " 个类不存在");
    }
}
