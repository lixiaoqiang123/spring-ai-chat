package com.lxq.spring_api_chat.agent.tool;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索工具
 * 模拟搜索功能（实际项目中应该调用真实的搜索API或RAG系统）
 */
@Component
public class SearchTool implements AgentTool {

    // 模拟知识库
    private static final Map<String, String> KNOWLEDGE_BASE = new HashMap<>();

    static {
        KNOWLEDGE_BASE.put("spring ai", "Spring AI 是 Spring 生态系统中用于构建 AI 应用的框架，提供了统一的 API 来集成各种 AI 模型。");
        KNOWLEDGE_BASE.put("java 21", "Java 21 是 LTS 版本，引入了虚拟线程(Virtual Threads)、记录模式(Record Patterns)等重要特性。");
        KNOWLEDGE_BASE.put("agent", "Agent 是具有自主决策能力的智能体，能够感知环境、制定计划并执行行动来完成目标。");
        KNOWLEDGE_BASE.put("rag", "RAG(Retrieval-Augmented Generation)是检索增强生成技术，通过检索相关文档来增强大模型的回答质量。");
        KNOWLEDGE_BASE.put("prompt engineering", "Prompt Engineering 是设计和优化提示词的技术，用于引导大模型生成更准确、更有用的回答。");
    }

    @Override
    public String getName() {
        return "search";
    }

    @Override
    public String getDescription() {
        return "在知识库中搜索相关信息。可以搜索技术概念、定义和说明。";
    }

    @Override
    public String getParameterDescription() {
        return "query: 要搜索的关键词或问题，例如: 'Spring AI', 'Java 21', 'Agent'";
    }

    @Override
    public String execute(String input) {
        String query = input.trim().toLowerCase();

        // 模糊匹配
        for (Map.Entry<String, String> entry : KNOWLEDGE_BASE.entrySet()) {
            if (query.contains(entry.getKey()) || entry.getKey().contains(query)) {
                return String.format("搜索结果: %s", entry.getValue());
            }
        }

        return String.format("未找到关于'%s'的相关信息。可搜索的主题包括: %s",
                input, String.join(", ", KNOWLEDGE_BASE.keySet()));
    }
}
