package com.lxq.spring_api_chat.example;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 流式API客户端示例
 * 演示如何使用WebClient调用流式聊天接口
 */
public class StreamChatClientExample {

    private static final String BASE_URL = "http://localhost:8080";
    private final WebClient webClient;

    public StreamChatClientExample() {
        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * 示例1: 基本的流式调用
     */
    public void basicStreamExample(String message) {
        System.out.println("=== 基本流式调用示例 ===");
        System.out.println("用户: " + message);
        System.out.print("AI: ");

        ChatRequest request = new ChatRequest(message, null);

        webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .doOnNext(event -> {
                    String eventType = event.event();
                    String data = (String) event.data();

                    if ("message".equals(eventType)) {
                        // 逐字打印AI回复
                        System.out.print(data);
                    } else if ("done".equals(eventType)) {
                        System.out.println("\n[流结束] " + data);
                    } else if ("error".equals(eventType)) {
                        System.err.println("\n[错误] " + data);
                    }
                })
                .doOnError(error -> System.err.println("\n请求失败: " + error.getMessage()))
                .doOnComplete(() -> System.out.println("\n=== 流完成 ==="))
                .blockLast(Duration.ofMinutes(5)); // 阻塞等待完成
    }

    /**
     * 示例2: 收集完整响应
     */
    public String collectFullResponse(String message) {
        System.out.println("=== 收集完整响应示例 ===");

        ChatRequest request = new ChatRequest(message, null);
        StringBuilder fullResponse = new StringBuilder();

        webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .filter(event -> "message".equals(event.event()))
                .map(event -> (String) event.data())
                .doOnNext(fullResponse::append)
                .blockLast(Duration.ofMinutes(5));

        return fullResponse.toString();
    }

    /**
     * 示例3: 带回调的流式处理
     */
    public void streamWithCallback(String message,
                                    OnMessageCallback onMessage,
                                    OnCompleteCallback onComplete,
                                    OnErrorCallback onError) {
        System.out.println("=== 回调方式处理流 ===");

        ChatRequest request = new ChatRequest(message, null);
        String[] sessionId = {null};

        webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .subscribe(
                        event -> {
                            String eventType = event.event();
                            String data = (String) event.data();

                            switch (eventType) {
                                case "message" -> onMessage.onMessage(data);
                                case "done" -> {
                                    // 解析会话ID
                                    if (data.contains("sessionId")) {
                                        sessionId[0] = extractSessionId(data);
                                    }
                                    onComplete.onComplete(sessionId[0]);
                                }
                                case "error" -> onError.onError(new Exception(data));
                            }
                        },
                        error -> onError.onError(error),
                        () -> System.out.println("订阅完成")
                );
    }

    /**
     * 示例4: 非阻塞流式处理
     */
    public Flux<String> getNonBlockingStream(String message) {
        System.out.println("=== 非阻塞流式处理 ===");

        ChatRequest request = new ChatRequest(message, null);

        return webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .filter(event -> "message".equals(event.event()))
                .map(event -> (String) event.data())
                .doOnNext(chunk -> System.out.print(chunk))
                .doOnError(error -> System.err.println("错误: " + error.getMessage()))
                .timeout(Duration.ofMinutes(5));
    }

    /**
     * 示例5: 批量处理
     */
    public void batchProcessing(String message) {
        System.out.println("=== 批量处理示例 ===");

        ChatRequest request = new ChatRequest(message, null);

        webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .filter(event -> "message".equals(event.event()))
                .map(event -> (String) event.data())
                .buffer(10) // 每10个片段处理一次
                .doOnNext(batch -> {
                    String batchContent = String.join("", batch);
                    System.out.println("[批次] " + batchContent);
                })
                .blockLast(Duration.ofMinutes(5));
    }

    /**
     * 示例6: 多流合并
     */
    public void mergeMultipleStreams(String message1, String message2) {
        System.out.println("=== 多流合并示例 ===");

        Flux<String> stream1 = getNonBlockingStream(message1)
                .map(content -> "[流1] " + content);

        Flux<String> stream2 = getNonBlockingStream(message2)
                .map(content -> "[流2] " + content);

        Flux.merge(stream1, stream2)
                .doOnNext(System.out::println)
                .blockLast(Duration.ofMinutes(5));
    }

    /**
     * 示例7: 带重试的流式调用
     */
    public void streamWithRetry(String message) {
        System.out.println("=== 带重试的流式调用 ===");

        ChatRequest request = new ChatRequest(message, null);

        webClient.post()
                .uri("/api/chat/stream")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .retry(3) // 失败时重试3次
                .filter(event -> "message".equals(event.event()))
                .map(event -> (String) event.data())
                .doOnNext(System.out::print)
                .blockLast(Duration.ofMinutes(5));
    }

    /**
     * 辅助方法: 从JSON字符串中提取会话ID
     */
    private String extractSessionId(String json) {
        try {
            return json.substring(
                    json.indexOf("\"sessionId\":\"") + 14,
                    json.lastIndexOf("\"}")
            );
        } catch (Exception e) {
            return null;
        }
    }

    // 回调接口定义
    @FunctionalInterface
    public interface OnMessageCallback {
        void onMessage(String content);
    }

    @FunctionalInterface
    public interface OnCompleteCallback {
        void onComplete(String sessionId);
    }

    @FunctionalInterface
    public interface OnErrorCallback {
        void onError(Throwable error);
    }

    /**
     * 主方法 - 运行所有示例
     */
    public static void main(String[] args) {
        StreamChatClientExample client = new StreamChatClientExample();

        // 示例1: 基本流式调用
        System.out.println("\n==========================================\n");
        client.basicStreamExample("介绍一下Java 21的新特性");

        // 等待一段时间
        sleep(2000);

        // 示例2: 收集完整响应
        System.out.println("\n==========================================\n");
        String fullResponse = client.collectFullResponse("什么是Spring AI?");
        System.out.println("完整响应: " + fullResponse);

        // 等待一段时间
        sleep(2000);

        // 示例3: 带回调的处理
        System.out.println("\n==========================================\n");
        client.streamWithCallback(
                "解释一下响应式编程",
                content -> System.out.print(content), // onMessage
                sessionId -> System.out.println("\n[完成] 会话ID: " + sessionId), // onComplete
                error -> System.err.println("\n[错误] " + error.getMessage()) // onError
        );

        // 等待流完成
        sleep(10000);

        // 示例4: 批量处理
        System.out.println("\n==========================================\n");
        client.batchProcessing("介绍Spring Boot 3.5的新功能");

        System.out.println("\n==========================================");
        System.out.println("所有示例执行完成!");
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
