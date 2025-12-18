package com.lxq.spring_api_chat.chat.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chat Memory 配置类
 * 配置聊天记忆存储，实现多轮对话上下文保持
 */
@Configuration
public class ChatMemoryConfig {

    /**
     * 创建内存型聊天记忆存储
     * 使用 MessageWindowChatMemory 配合 InMemoryChatMemoryRepository 在内存中存储对话历史
     *
     * 特点：
     * - 简单快速，适合开发和测试
     * - 应用重启后数据丢失
     * - 支持滑动窗口，自动管理消息数量
     * - 后续可以替换为 Redis、数据库等持久化存储
     *
     * @return ChatMemory 实例
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)  // 设置最大消息数，超过后自动删除最旧的消息
                .build();
    }
}
