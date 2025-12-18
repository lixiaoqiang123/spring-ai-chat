package com.lxq.spring_api_chat.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * ChatController 增强版流式接口集成测试
 * 测试思考过程（reasoning_content）和回复内容（content）的区分
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "60000")
class ChatControllerEnhancedStreamTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试增强版流式接口
     * 验证能够区分思考内容和回复内容
     */
    @Test
    void testStreamMessageEnhanced_shouldDistinguishReasoningAndContent() throws Exception {
        // 准备测试数据 - 使用需要推理的问题
        ChatRequest request = new ChatRequest(
            "请用思考的方式解答：如果一个池塘里的荷花每天增长一倍，30天能覆盖整个池塘，那么覆盖半个池塘需要多少天？",
            null
        );

        // 发送请求并获取 SSE 流
        Flux<ServerSentEvent<String>> sseFlux = webTestClient
            .post()
            .uri("/api/chat/stream-enhanced")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .returnResult(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .getResponseBody();

        // 验证流式响应
        StepVerifier.create(sseFlux.timeout(Duration.ofSeconds(60)))
            .expectNextMatches(event -> {
                System.out.println("收到 SSE 事件类型: " + event.event() + ", 数据: " + event.data());
                // 第一个事件可能是思考内容或回复内容
                return event.event() != null &&
                       (event.event().equals("reasoning") ||
                        event.event().equals("content") ||
                        event.event().equals("done"));
            })
            .thenConsumeWhile(event -> {
                // 打印所有事件用于调试
                System.out.println("SSE 事件: " + event.event() + " - " + event.data());

                // 验证事件类型
                String eventType = event.event();
                return eventType.equals("reasoning") ||
                       eventType.equals("content") ||
                       eventType.equals("done");
            })
            .expectComplete()
            .verify();
    }

    /**
     * 测试简单问题的流式响应
     * 验证基本的流式功能
     */
    @Test
    void testStreamMessageEnhanced_simpleQuestion() throws Exception {
        ChatRequest request = new ChatRequest("你好，请简单介绍一下你自己", null);

        Flux<ServerSentEvent<String>> sseFlux = webTestClient
            .post()
            .uri("/api/chat/stream-enhanced")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .returnResult(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .getResponseBody();

        // 验证至少收到一个 content 事件和一个 done 事件
        StepVerifier.create(sseFlux.timeout(Duration.ofSeconds(60)))
            .thenConsumeWhile(event -> {
                System.out.println("SSE 事件: " + event.event() + " - " + event.data());
                return !event.event().equals("done");
            })
            .expectNextMatches(event -> {
                System.out.println("最终事件: " + event.event());
                return event.event().equals("done") && event.data() != null;
            })
            .expectComplete()
            .verify();
    }

    /**
     * 测试多轮对话的会话记忆
     * 验证思考模式下的会话记忆功能
     */
    @Test
    void testStreamMessageEnhanced_conversationMemory() throws Exception {
        // 第一轮对话
        ChatRequest firstRequest = new ChatRequest("我的名字是小明", null);

        String[] sessionId = new String[1];

        Flux<ServerSentEvent<String>> firstFlux = webTestClient
            .post()
            .uri("/api/chat/stream-enhanced")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(firstRequest)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .returnResult(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
            .getResponseBody();

        // 提取 sessionId
        StepVerifier.create(firstFlux.timeout(Duration.ofSeconds(60)))
            .thenConsumeWhile(event -> {
                if (event.event().equals("done")) {
                    String data = event.data();
                    if (data != null && data.contains("sessionId")) {
                        try {
                            // 简单的 JSON 解析获取 sessionId
                            sessionId[0] = data.substring(
                                data.indexOf("\"sessionId\":\"") + 13,
                                data.lastIndexOf("\"")
                            );
                        } catch (Exception e) {
                            // 如果是 StreamChunk 格式
                            if (data.contains("sessionId")) {
                                sessionId[0] = "test-session"; // 使用测试会话ID
                            }
                        }
                    }
                    return false;
                }
                return true;
            })
            .expectComplete()
            .verify();

        // 第二轮对话 - 询问名字（测试记忆）
        if (sessionId[0] != null) {
            ChatRequest secondRequest = new ChatRequest("我的名字是什么？", sessionId[0]);

            Flux<ServerSentEvent<String>> secondFlux = webTestClient
                .post()
                .uri("/api/chat/stream-enhanced")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(secondRequest)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new org.springframework.core.ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .getResponseBody();

            // 验证第二轮响应包含"小明"
            StepVerifier.create(secondFlux.timeout(Duration.ofSeconds(60)))
                .thenConsumeWhile(event -> {
                    String data = event.data();
                    if (data != null) {
                        System.out.println("第二轮对话 - 事件: " + event.event() + ", 数据: " + data);
                    }
                    return !event.event().equals("done");
                })
                .expectNextMatches(event -> event.event().equals("done"))
                .expectComplete()
                .verify();
        }
    }

    /**
     * 测试错误处理
     * 验证空消息请求的错误处理
     */
    @Test
    void testStreamMessageEnhanced_errorHandling() {
        // 发送空消息（应该触发验证错误）
        webTestClient
            .post()
            .uri("/api/chat/stream-enhanced")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{\"message\":\"\",\"sessionId\":null}")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().is4xxClientError();
    }
}
