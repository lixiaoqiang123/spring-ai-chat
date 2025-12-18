package com.lxq.spring_api_chat.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 流式响应数据块
 * 用于区分思考内容（reasoning_content）和实际回复内容（content）
 *
 * @param type 数据类型：reasoning（思考过程）、content（回复内容）、done（完成）、error（错误）
 * @param data 数据内容
 * @param sessionId 会话ID（仅在 done 事件中包含）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamChunk(
    String type,
    String data,
    String sessionId
) {
    /**
     * 创建思考内容块
     */
    public static StreamChunk reasoning(String content) {
        return new StreamChunk("reasoning", content, null);
    }

    /**
     * 创建回复内容块
     */
    public static StreamChunk content(String content) {
        return new StreamChunk("content", content, null);
    }

    /**
     * 创建完成事件
     */
    public static StreamChunk done(String sessionId) {
        return new StreamChunk("done", null, sessionId);
    }

    /**
     * 创建错误事件
     */
    public static StreamChunk error(String message) {
        return new StreamChunk("error", message, null);
    }
}
