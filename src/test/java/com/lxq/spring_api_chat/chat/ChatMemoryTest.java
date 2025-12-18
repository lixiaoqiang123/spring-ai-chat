package com.lxq.spring_api_chat.chat;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import com.lxq.spring_api_chat.chat.dto.ChatResponse;
import com.lxq.spring_api_chat.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Chat Memory 功能测试
 * 验证多轮对话记忆功能是否正常工作
 */
@SpringBootTest
public class ChatMemoryTest {

    @Autowired
    private ChatService chatService;

    /**
     * 测试基本的多轮对话记忆
     * 验证 AI 能够记住之前的对话内容
     */
    @Test
    public void testBasicMemory() {
        // 生成唯一的会话ID
        String sessionId = "test-session-" + System.currentTimeMillis();

        // 第一轮对话：告诉 AI 一个信息
        ChatRequest request1 = new ChatRequest("我的名字叫张三", sessionId);
        ChatResponse response1 = chatService.chat(request1);
        assertNotNull(response1);
        assertNotNull(response1.getReply());
        assertEquals(sessionId, response1.getSessionId());
        System.out.println("第一轮对话:");
        System.out.println("用户: " + request1.message());
        System.out.println("AI: " + response1.getReply());
        System.out.println();

        // 第二轮对话：询问之前的信息
        ChatRequest request2 = new ChatRequest("我的名字是什么？", sessionId);
        ChatResponse response2 = chatService.chat(request2);
        assertNotNull(response2);
        assertNotNull(response2.getReply());
        assertEquals(sessionId, response2.getSessionId());
        System.out.println("第二轮对话:");
        System.out.println("用户: " + request2.message());
        System.out.println("AI: " + response2.getReply());
        System.out.println();

        // 验证 AI 的回复中包含"张三"
        assertTrue(response2.getReply().contains("张三"),
                "AI 应该能够记住用户的名字");

        // 清理测试数据
        chatService.clearMemory(sessionId);
    }

    /**
     * 测试会话隔离
     * 验证不同会话的记忆是独立的
     */
    @Test
    public void testSessionIsolation() {
        // 创建两个不同的会话
        String sessionId1 = "test-session-1-" + System.currentTimeMillis();
        String sessionId2 = "test-session-2-" + System.currentTimeMillis();

        // 会话1：告诉 AI 喜欢苹果
        ChatRequest request1 = new ChatRequest("我喜欢吃苹果", sessionId1);
        ChatResponse response1 = chatService.chat(request1);
        assertNotNull(response1);
        System.out.println("会话1 - 第一轮:");
        System.out.println("用户: " + request1.message());
        System.out.println("AI: " + response1.getReply());
        System.out.println();

        // 会话2：告诉 AI 喜欢香蕉
        ChatRequest request2 = new ChatRequest("我喜欢吃香蕉", sessionId2);
        ChatResponse response2 = chatService.chat(request2);
        assertNotNull(response2);
        System.out.println("会话2 - 第一轮:");
        System.out.println("用户: " + request2.message());
        System.out.println("AI: " + response2.getReply());
        System.out.println();

        // 在会话1中询问
        ChatRequest request3 = new ChatRequest("我喜欢吃什么水果？", sessionId1);
        ChatResponse response3 = chatService.chat(request3);
        assertNotNull(response3);
        System.out.println("会话1 - 第二轮:");
        System.out.println("用户: " + request3.message());
        System.out.println("AI: " + response3.getReply());
        System.out.println();

        // 验证会话1的回复中包含"苹果"而不包含"香蕉"
        assertTrue(response3.getReply().contains("苹果"),
                "会话1应该记住用户喜欢苹果");
        assertFalse(response3.getReply().contains("香蕉"),
                "会话1不应该知道会话2的内容");

        // 清理测试数据
        chatService.clearMemory(sessionId1);
        chatService.clearMemory(sessionId2);
    }

    /**
     * 测试清除记忆功能
     * 验证清除记忆后 AI 无法记住之前的对话
     */
    @Test
    public void testClearMemory() {
        String sessionId = "test-session-clear-" + System.currentTimeMillis();

        // 第一轮对话：告诉 AI 一个信息
        ChatRequest request1 = new ChatRequest("我住在北京", sessionId);
        ChatResponse response1 = chatService.chat(request1);
        assertNotNull(response1);
        System.out.println("清除前 - 第一轮:");
        System.out.println("用户: " + request1.message());
        System.out.println("AI: " + response1.getReply());
        System.out.println();

        // 清除记忆
        chatService.clearMemory(sessionId);
        System.out.println("已清除会话记忆");
        System.out.println();

        // 清除后询问
        ChatRequest request2 = new ChatRequest("我住在哪里？", sessionId);
        ChatResponse response2 = chatService.chat(request2);
        assertNotNull(response2);
        System.out.println("清除后 - 第二轮:");
        System.out.println("用户: " + request2.message());
        System.out.println("AI: " + response2.getReply());
        System.out.println();

        // 验证 AI 无法回答（因为记忆已被清除）
        // 注意：这个断言可能不够准确，因为 AI 可能会说"我不知道"或类似的话
        assertFalse(response2.getReply().contains("北京"),
                "清除记忆后，AI 不应该记住之前的信息");

        // 清理测试数据
        chatService.clearMemory(sessionId);
    }

    /**
     * 测试多轮复杂对话
     * 验证 AI 能够在多轮对话中保持上下文
     */
    @Test
    public void testMultiTurnConversation() {
        String sessionId = "test-session-multi-" + System.currentTimeMillis();

        // 第一轮：介绍背景
        ChatRequest request1 = new ChatRequest("我是一名软件工程师，正在学习 Spring AI", sessionId);
        ChatResponse response1 = chatService.chat(request1);
        assertNotNull(response1);
        System.out.println("第一轮:");
        System.out.println("用户: " + request1.message());
        System.out.println("AI: " + response1.getReply());
        System.out.println();

        // 第二轮：询问相关问题
        ChatRequest request2 = new ChatRequest("我应该先学习哪些核心概念？", sessionId);
        ChatResponse response2 = chatService.chat(request2);
        assertNotNull(response2);
        System.out.println("第二轮:");
        System.out.println("用户: " + request2.message());
        System.out.println("AI: " + response2.getReply());
        System.out.println();

        // 第三轮：继续深入
        ChatRequest request3 = new ChatRequest("我的职业是什么？", sessionId);
        ChatResponse response3 = chatService.chat(request3);
        assertNotNull(response3);
        System.out.println("第三轮:");
        System.out.println("用户: " + request3.message());
        System.out.println("AI: " + response3.getReply());
        System.out.println();

        // 验证 AI 记住了职业信息
        assertTrue(response3.getReply().contains("软件工程师") ||
                        response3.getReply().contains("工程师"),
                "AI 应该记住用户的职业");

        // 清理测试数据
        chatService.clearMemory(sessionId);
    }
}
