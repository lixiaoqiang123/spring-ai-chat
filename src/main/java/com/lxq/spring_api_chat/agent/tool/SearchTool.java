package com.lxq.spring_api_chat.agent.tool;

import com.lxq.spring_api_chat.rag.dto.RetrievalResult;
import com.lxq.spring_api_chat.rag.service.DocumentRetrievalService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 搜索工具 - RAG版本
 * 使用向量检索技术从知识库中搜索相关信息
 */
@Component
public class SearchTool implements AgentTool {

    private final DocumentRetrievalService retrievalService;

    public SearchTool(DocumentRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "在知识库中搜索相关信息。使用向量检索技术，能够理解语义并返回最相关的文档内容。";
    }

    @Override
    public String getParameterDescription() {
        return "query: 要搜索的问题或关键词，例如: 'Spring AI是什么', 'RAG技术原理', 'Agent如何工作'";
    }

    @Override
    public String execute(String input) {
        String query = input.trim();

        if (query.isEmpty()) {
            return "错误: 搜索查询不能为空";
        }

        try {
            // 使用RAG检索，返回Top 3最相关的文档
            RetrievalResult result = retrievalService.retrieve(query, 3, 0.7);

            if (!result.success()) {
                return String.format("搜索失败: %s", result.errorMessage());
            }

            if (result.documents().isEmpty()) {
                return String.format("未找到关于'%s'的相关信息。\n提示: 可能需要先索引相关文档到知识库。", query);
            }

            // 格式化检索结果
            StringBuilder response = new StringBuilder();
            response.append(String.format("找到 %d 条相关信息:\n\n", result.count()));

            List<Document> docs = result.documents();
            for (int i = 0; i < docs.size(); i++) {
                Document doc = docs.get(i);
                String source = doc.getMetadata().getOrDefault("source", "未知").toString();
                String content = doc.getText();

                // 限制内容长度，避免返回过长的文本
                if (content.length() > 300) {
                    content = content.substring(0, 300) + "...";
                }

                response.append(String.format("[%d] 来源: %s\n%s\n\n",
                    i + 1, source, content));
            }

            response.append(String.format("(检索耗时: %dms)", result.duration()));

            return response.toString();

        } catch (Exception e) {
            return String.format("搜索过程中发生错误: %s", e.getMessage());
        }
    }
}
