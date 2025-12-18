package com.lxq.spring_api_chat.chat.dto;

/**
 * 聊天响应数据传输对象
 */
public class ChatResponse {

    /**
     * AI 模型返回的回复内容
     */
    private String reply;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 响应时间戳
     */
    private Long timestamp;

    public ChatResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ChatResponse(String reply, String sessionId) {
        this.reply = reply;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "reply='" + reply + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
