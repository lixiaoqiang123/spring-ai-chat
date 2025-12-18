package com.lxq.spring_api_chat.chat.dto;

/**
 * 聊天请求数据传输对象
 * 使用 Java 21 Record 特性简化代码
 *
 * @param message 用户输入的消息内容
 * @param sessionId 会话ID，用于保持对话上下文（可选）
 */
public record ChatRequest(String message, String sessionId) {

    /**
     * 验证消息内容不为空
     */
    public ChatRequest {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
    }

    /**
     * 便捷构造方法 - 不指定会话ID
     *
     * @param message 用户消息
     */
    public ChatRequest(String message) {
        this(message, null);
    }
}
