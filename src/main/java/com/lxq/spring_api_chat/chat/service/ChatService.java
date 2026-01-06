package com.lxq.spring_api_chat.chat.service;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import com.lxq.spring_api_chat.chat.dto.ChatResponse;
import com.lxq.spring_api_chat.chat.dto.StreamChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 聊天服务类
 * 负责处理与 AI 模型的对话交互，支持多轮对话记忆
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    /**
     * 构造函数注入 ChatModel、ChatMemory 和 VectorStore
     * 创建带有记忆功能的 ChatClient
     *
     * @param chatModel Spring AI 提供的聊天模型
     * @param chatMemory 聊天记忆存储
     * @param vectorStore 向量存储，用于RAG功能
     */
    public ChatService(ChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;

        // 创建带有 Memory Advisor 的 ChatClient
        // MessageChatMemoryAdvisor 会自动管理对话历史的存储和检索
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(memoryAdvisor)
                .build();
    }

    /**
     * 发送消息到 AI 模型并获取回复
     * 支持多轮对话记忆，通过 sessionId 区分不同会话
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        // 如果没有会话ID，生成一个新的
        final String sessionId = (request.sessionId() == null || request.sessionId().trim().isEmpty())
                ? UUID.randomUUID().toString()
                : request.sessionId();

        // 调用 AI 模型进行对话
        // 通过 advisorParams 传递会话ID
        // MessageChatMemoryAdvisor 会自动：
        // 1. 从 chatMemory 中检索该会话的历史消息
        // 2. 将历史消息添加到当前请求的上下文中
        // 3. 将当前对话（用户消息和AI回复）存储到 chatMemory
        String reply = chatClient.prompt()
                .user(request.message())
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .call()
                .content();

        // 构造并返回响应
        return new ChatResponse(reply, sessionId);
    }

    /**
     * 流式对话方法 - 返回响应式流（增强版）
     * 区分思考内容（reasoning_content）和实际回复内容（content）
     * 适用于支持思考模式的模型（如 GLM-4.6）
     * 支持多轮对话记忆
     *
     * @param request 聊天请求
     * @return StreamChunk 流，包含思考内容和回复内容的类型化数据块
     */
    public Flux<StreamChunk> chatStreamEnhanced(ChatRequest request) {
        // 获取或生成会话ID
        final String sessionId = getOrGenerateSessionId(request);
        System.out.println("ChatService.chatStreamEnhanced - 使用会话ID: " + sessionId + ", 消息: " + request.message());

        // 使用 Spring AI 的 stream().chatResponse() 方法获取完整的 ChatResponse 对象
        // 这样可以访问 metadata 中的 reasoningContent
        return chatClient.prompt()
                .user(request.message())
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .stream()
                .chatResponse()
                // 将每个 ChatResponse 转换为 StreamChunk
                .flatMap(response -> {
                    var result = response.getResult();
                    if (result == null || result.getOutput() == null) {
                        return Flux.empty();
                    }

                    var message = result.getOutput();
                    var metadata = message.getMetadata();

                    // 调试日志：打印 metadata 的所有键
                    if (metadata != null) {
                        System.out.println("[DEBUG] Metadata keys: " + metadata.keySet());
                        metadata.forEach((key, value) -> {
                            if (key.toLowerCase().contains("reason")) {
                                System.out.println("[DEBUG] " + key + " = " + value);
                            }
                        });
                    }

                    // 创建一个流来发送思考内容和回复内容
                    return Flux.<StreamChunk>create(sink -> {
                        // 1. 首先发送思考内容（如果存在）
                        // 尝试多种可能的 key 名称
                        if (metadata != null) {
                            String reasoningContent = null;

                            // 尝试不同的 key 名称
                            if (metadata.containsKey("reasoningContent")) {
                                reasoningContent = (String) metadata.get("reasoningContent");
                            } else if (metadata.containsKey("reasoning_content")) {
                                reasoningContent = (String) metadata.get("reasoning_content");
                            } else if (metadata.containsKey("thinking")) {
                                reasoningContent = (String) metadata.get("thinking");
                            }

                            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                                System.out.println("[DEBUG] 发送思考内容，长度: " + reasoningContent.length());
                                sink.next(StreamChunk.reasoning(reasoningContent));
                            }
                        }

                        // 2. 然后发送实际回复内容
                        String content = message.getText();
                        if (content != null && !content.isEmpty()) {
                            System.out.println("[DEBUG] 发送回复内容，长度: " + content.length());
                            sink.next(StreamChunk.content(content));
                        }

                        sink.complete();
                    });
                })
                // 在流结束时添加完成事件
                .concatWith(Flux.just(StreamChunk.done(sessionId)))
                // 错误处理
                .doOnError(error ->
                    System.err.println("流式对话发生错误: " + error.getMessage())
                )
                // 完成时的日志
                .doOnComplete(() ->
                    System.out.println("流式对话完成 - 会话ID: " + sessionId + ", 消息: " + request.message())
                );
    }

    /**
     * 流式对话方法 - 返回响应式流
     * 使用 Spring AI 的 stream() 方法实现真正的流式返回
     * 适用于 SSE (Server-Sent Events) 场景,可以逐步返回AI生成的内容
     * 支持多轮对话记忆
     *
     * @param request 聊天请求
     * @return AI 回复内容的响应式流,每个元素是一个文本片段
     */
    public Flux<String> chatStream(ChatRequest request) {
        // 获取或生成会话ID
        final String sessionId = getOrGenerateSessionId(request);
        System.out.println("ChatService.chatStream - 使用会话ID: " + sessionId + ", 消息: " + request.message());

        // 使用 Spring AI 的 stream() 方法进行流式调用
        // stream() 返回 Flux<ChatResponse>,需要提取其中的内容
        // 同样支持 Memory Advisor，会自动管理对话历史
        return chatClient.prompt()
                .user(request.message())
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .stream()
                .content()
                // 错误处理:在流中发生错误时记录并传播
                .doOnError(error ->
                    System.err.println("流式对话发生错误: " + error.getMessage())
                )
                // 完成时的日志
                .doOnComplete(() ->
                    System.out.println("流式对话完成 - 会话ID: " + sessionId + ", 消息: " + request.message())
                );
    }

    /**
     * 获取或生成会话ID
     * 用于在流式响应��保持会话一致性
     *
     * @param request 聊天请求
     * @return 会话ID
     */
    public String getOrGenerateSessionId(ChatRequest request) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        return sessionId;
    }

    /**
     * 清除指定会话的记忆
     * 用于重置对话上下文
     *
     * @param sessionId 会话ID
     */
    public void clearMemory(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            chatMemory.clear(sessionId);
        }
    }

    /**
     * 清除所有会话的记忆
     * 谨慎使用，会删除所有对话历史
     */
    public void clearAllMemory() {
        // InMemoryChatMemory 没有直接的 clearAll 方法
        // 如果需要，可以重新创建 ChatMemory 实例
        System.out.println("警告：清除所有记忆需要重启应用或重新注入 ChatMemory");
    }

    /**
     * RAG增强对话 - 同步版本（手动实现）
     * 结合向量检索和对话记忆，基于知识库内容生成答案
     *
     * @param request 聊天请求
     * @param topK 检索的文档数量，默认5
     * @param similarityThreshold 相似度阈值，默认0.7
     * @return 聊天响应
     */
    public ChatResponse chatWithRag(ChatRequest request, int topK, double similarityThreshold) {
        // 获取或生成会话ID
        final String sessionId = getOrGenerateSessionId(request);
        System.out.println("ChatService.chatWithRag - 使用会话ID: " + sessionId + ", 消息: " + request.message());

        // 1. 检索相关文档
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(request.message())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build()
        );

        // 2. 构建上下文
        String context = buildContext(documents);

        // 3. 构建增强的Prompt
        String enhancedPrompt;
        if (!documents.isEmpty()) {
            enhancedPrompt = String.format("""
                请基于以下上下文信息回答问题。如果上下文中没有相关信息，请明确告知用户。

                上下文信息：
                %s

                用户问题：%s
                """, context, request.message());
        } else {
            enhancedPrompt = String.format("""
                注意：向量数据库中未找到与问题相关的文档。

                用户问题：%s

                请根据你的知识回答，并告知用户这不是基于文档库的回答。
                """, request.message());
        }
        log.info("context:{}",context);
        log.info("enhancedPrompt:{}",enhancedPrompt);
        // 4. 调用AI模型（带记忆）
        String reply = chatClient.prompt()
                .user(enhancedPrompt)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .call()
                .content();

        // 构造并返回响应
        return new ChatResponse(reply, sessionId);
    }

    /**
     * RAG增强流式对话（手动实现）
     * 结合向量检索和对话记忆，以流式方式返回基于知识库内容的答案
     *
     * @param request 聊天请求
     * @param topK 检索的文档数量，默认5
     * @param similarityThreshold 相似度阈值，默认0.7
     * @return AI回复内容的响应式流
     */
    public Flux<String> chatWithRagStream(ChatRequest request, int topK, double similarityThreshold) {
        // 获取或生成会话ID
        final String sessionId = getOrGenerateSessionId(request);
        System.out.println("ChatService.chatWithRagStream - 使用会话ID: " + sessionId + ", 消息: " + request.message());

        // 1. 检索相关文档
        List<Document> documents = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(request.message())
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build()
        );

        // 2. 构建上下文
        String context = buildContext(documents);

        // 3. 构建增强的Prompt
        String enhancedPrompt;
        if (!documents.isEmpty()) {
            enhancedPrompt = String.format("""
                请基于以下上下文信息回答问题。如果上下文中没有相关信息，请明确告知用户。

                上下文信息：
                %s

                用户问题：%s
                """, context, request.message());
        } else {
            enhancedPrompt = String.format("""
                注意：向量数据库中未找到与问题相关的文档。

                用户问题：%s

                请根据你的知识回答，并告知用户这不是基于文档库的回答。
                """, request.message());
        }

        // 4. 使用流式调用
        return chatClient.prompt()
                .user(enhancedPrompt)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                )
                .stream()
                .content()
                // 错误处理
                .doOnError(error ->
                    System.err.println("RAG流式对话发生错误: " + error.getMessage())
                )
                // 完成时的日志
                .doOnComplete(() ->
                    System.out.println("RAG流式对话完成 - 会话ID: " + sessionId + ", 消息: " + request.message())
                );
    }

    /**
     * 构建上下文字符串
     * 将检索到的文档格式化为上下文
     */
    private String buildContext(List<Document> documents) {
        return documents.stream()
            .map(doc -> {
                String source = doc.getMetadata().getOrDefault("source", "未知来源").toString();
                return String.format("[来源: %s]\n%s", source, doc.getText());
            })
            .collect(Collectors.joining("\n\n---\n\n"));
    }
}
