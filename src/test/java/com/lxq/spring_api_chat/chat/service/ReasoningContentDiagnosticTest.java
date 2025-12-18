package com.lxq.spring_api_chat.chat.service;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import com.lxq.spring_api_chat.chat.dto.StreamChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 诊断测试：验证 GLM-4.6 的 reasoning_content 是否被正确提取
 *
 * 根据 API 响应体分析：
 * - 思考内容在 choices[0].delta.reasoning_content 中
 * - 回复内容在 choices[0].delta.content 中
 *
 * 本测试用于验证 Spring AI 是否正确将 reasoning_content 映射到 metadata
 */
@SpringBootTest
public class ReasoningContentDiagnosticTest {

    @Autowired
    private ChatService chatService;

    @Test
    public void testReasoningContentExtraction() {
        System.out.println("\n========================================");
        System.out.println("开始诊断测试：验证 reasoning_content 提取");
        System.out.println("========================================\n");

        // 创建测试请求
        ChatRequest request = new ChatRequest("你好", null);

        // 调用增强版流式服务
        Flux<StreamChunk> responseFlux = chatService.chatStreamEnhanced(request);

        // 收集所有响应块
        List<StreamChunk> chunks = new ArrayList<>();

        // 订阅并收集结果
        StepVerifier.create(responseFlux)
                .recordWith(() -> chunks)
                .thenConsumeWhile(chunk -> {
                    System.out.println("\n收到 StreamChunk:");
                    System.out.println("  类型: " + chunk.type());
                    System.out.println("  数据长度: " + (chunk.data() != null ? chunk.data().length() : 0));
                    if (chunk.data() != null && chunk.data().length() < 100) {
                        System.out.println("  数据内容: " + chunk.data());
                    }
                    System.out.println("  会话ID: " + chunk.sessionId());
                    return true;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        // 分析结果
        System.out.println("\n========================================");
        System.out.println("诊断结果分析");
        System.out.println("========================================\n");

        long reasoningCount = chunks.stream()
                .filter(chunk -> "reasoning".equals(chunk.type()))
                .count();

        long contentCount = chunks.stream()
                .filter(chunk -> "content".equals(chunk.type()))
                .count();

        long doneCount = chunks.stream()
                .filter(chunk -> "done".equals(chunk.type()))
                .count();

        System.out.println("统计信息:");
        System.out.println("  reasoning 块数量: " + reasoningCount);
        System.out.println("  content 块数量: " + contentCount);
        System.out.println("  done 块数量: " + doneCount);
        System.out.println("  总块数量: " + chunks.size());

        if (reasoningCount == 0) {
            System.out.println("\n⚠️ 警告：未检测到 reasoning 类型的块！");
            System.out.println("可能的原因：");
            System.out.println("  1. Spring AI 未正确提取 reasoning_content 字段");
            System.out.println("  2. API 响应中没有 reasoning_content");
            System.out.println("  3. metadata 映射配置有问题");
        } else {
            System.out.println("\n✅ 成功：检测到 " + reasoningCount + " 个 reasoning 块");

            // 打印第一个 reasoning 块的内容
            chunks.stream()
                    .filter(chunk -> "reasoning".equals(chunk.type()))
                    .findFirst()
                    .ifPresent(chunk -> {
                        System.out.println("\n第一个 reasoning 块内容:");
                        System.out.println(chunk.data());
                    });
        }

        System.out.println("\n========================================\n");
    }

    @Test
    public void testBasicStreamResponse() {
        System.out.println("\n========================================");
        System.out.println("基础流式响应测试");
        System.out.println("========================================\n");

        ChatRequest request = new ChatRequest("什么是 Spring AI?", null);
        Flux<StreamChunk> responseFlux = chatService.chatStreamEnhanced(request);

        StepVerifier.create(responseFlux)
                .expectNextMatches(chunk -> {
                    System.out.println("收到块类型: " + chunk.type());
                    return chunk.type() != null;
                })
                .thenConsumeWhile(chunk -> {
                    System.out.println("收到块类型: " + chunk.type());
                    return true;
                })
                .expectComplete()
                .verify(Duration.ofSeconds(30));

        System.out.println("\n========================================\n");
    }
}
