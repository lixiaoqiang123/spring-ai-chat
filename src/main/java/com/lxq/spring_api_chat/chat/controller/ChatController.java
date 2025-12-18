package com.lxq.spring_api_chat.chat.controller;

import com.lxq.spring_api_chat.chat.dto.ChatRequest;
import com.lxq.spring_api_chat.chat.dto.ChatResponse;
import com.lxq.spring_api_chat.chat.dto.StreamChunk;
import com.lxq.spring_api_chat.chat.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 聊天接口控制器
 * 提供与 AI 模型对话的 REST API
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    /**
     * 构造函数注入 ChatService
     *
     * @param chatService 聊天服务
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 发送消息到 AI 模型
     *
     * @param request 聊天请求，包含用户消息和可选的会话ID
     * @return 聊天响应，包含 AI 回复内容
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        try {
            ChatResponse response = chatService.chat(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // 返回 400 错误和错误消息
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("错误: " + e.getMessage(), null));
        } catch (Exception e) {
            // 返回 500 错误和错误消息
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("服务器错误: " + e.getMessage(), null));
        }
    }

    /**
     * 简单的对话接口，只接收消息文本
     *
     * @param message 用户消息
     * @return AI 回复内容
     */
    @GetMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestParam String message) {
        try {
            ChatRequest request = new ChatRequest(message, null);
            ChatResponse response = chatService.chat(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("错误: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("服务器错误: " + e.getMessage(), null));
        }
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is running");
    }

    /**
     * 增强版流式对话接口 - Server-Sent Events (SSE)
     * 支持区分思考内容（reasoning_content）和实际回复内容（content）
     * 适用于支持思考模式的模型（如 GLM-4.6）
     *
     * 前端接收到的事件类型：
     * - reasoning: 思考过程内容
     * - content: 实际回复内容
     * - done: 对话完成（包含 sessionId）
     * - error: 错误信息
     *
     * @param request 聊天请求,包含用户消息和可选的会话ID
     * @return SSE流,每个事件包含类型化的数据块
     */
    @PostMapping(value = "/stream-enhanced", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamChunk>> streamMessageEnhanced(@RequestBody ChatRequest request) {
        try {
            // 获取或生成会话ID
            String sessionId = chatService.getOrGenerateSessionId(request);
            System.out.println("增强版流式对话 - 会话ID: " + sessionId + ", 消息: " + request.message());

            // 创建包含正确 sessionId 的新请求对象
            ChatRequest requestWithSession = new ChatRequest(request.message(), sessionId);

            // 调用增强版流式服务,返回类型化的数据块流
            Flux<StreamChunk> chunkStream = chatService.chatStreamEnhanced(requestWithSession);

            // 将 StreamChunk 流转换为 SSE 格式
            return chunkStream
                    .map(chunk -> ServerSentEvent.<StreamChunk>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event(chunk.type()) // 使用 chunk 的类型作为 SSE 事件类型
                            .data(chunk)          // 发送完整的 StreamChunk 对象
                            .build())
                    // 错误处理:捕获流中的异常并返回错误事件
                    .onErrorResume(error -> Flux.just(
                            ServerSentEvent.<StreamChunk>builder()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .event("error")
                                    .data(StreamChunk.error("错误: " + error.getMessage()))
                                    .build()
                    ))
                    // 设置超时时间
                    .timeout(Duration.ofMinutes(5));
        } catch (Exception e) {
            // 处理同步异常,返回错误事件流
            return Flux.just(
                    ServerSentEvent.<StreamChunk>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event("error")
                            .data(StreamChunk.error("服务器错误: " + e.getMessage()))
                            .build()
            );
        }
    }

    /**
     * 流式对话接口 - Server-Sent Events (SSE)
     * 使用响应式流逐步返回AI生成的内容,适合实时显示
     * 前端可以通过EventSource或fetch监听流式响应
     *
     * @param request 聊天请求,包含用户消息和可选的会话ID
     * @return SSE流,每个事件包含一个文本片段
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamMessage(@RequestBody ChatRequest request) {
        try {
            // 获取或生成会话ID
            String sessionId = chatService.getOrGenerateSessionId(request);
            System.out.println("流式对话 - 会话ID: " + sessionId + ", 消息: " + request.message());

            // 创建包含正确 sessionId 的新请求对象
            // 这样确保 Controller 和 Service 使用相同的 sessionId
            ChatRequest requestWithSession = new ChatRequest(request.message(), sessionId);

            // 调用流式服务,返回内容流
            Flux<String> contentStream = chatService.chatStream(requestWithSession);

            // 将内容流转换为SSE格式
            return contentStream
                    .map(content -> ServerSentEvent.<String>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event("message") // SSE事件类型
                            .data(content)    // 实际内容
                            .build())
                    // 在流结束时发送一个特殊的完成事件,包含会话ID
                    // 这样前端可以获取 sessionId 用于后续对话
                    .concatWith(Flux.just(
                            ServerSentEvent.<String>builder()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .event("done")
                                    .data("{\"sessionId\":\"" + sessionId + "\"}")
                                    .build()
                    ))
                    // 错误处理:捕获流中的异常并返回错误事件
                    .onErrorResume(error -> Flux.just(
                            ServerSentEvent.<String>builder()
                                    .id(String.valueOf(System.currentTimeMillis()))
                                    .event("error")
                                    .data("错误: " + error.getMessage())
                                    .build()
                    ))
                    // 设置心跳,防止连接超时(每30秒发送一次心跳)
                    .timeout(Duration.ofMinutes(5));
        } catch (Exception e) {
            // 处理同步异常,返回错误事件流
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .event("error")
                            .data("服务器错误: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 简单的流式对话接口 - 使用GET请求
     * 适用于快速测试,只需要消息内容
     *
     * @param message 用户消息
     * @return SSE流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAsk(@RequestParam String message) {
        ChatRequest request = new ChatRequest(message, null);
        return streamMessage(request);
    }

    /**
     * 清除指定会话的记忆
     * 用于重置对话上下文，开始新的对话
     *
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @DeleteMapping("/memory/{sessionId}")
    public ResponseEntity<String> clearMemory(@PathVariable String sessionId) {
        try {
            chatService.clearMemory(sessionId);
            return ResponseEntity.ok("会话 " + sessionId + " 的记忆已清除");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("清除记忆失败: " + e.getMessage());
        }
    }

    /**
     * 清除所有会话的记忆
     * 谨慎使用，会删除所有对话历史
     *
     * @return 操作结果
     */
    @DeleteMapping("/memory/all")
    public ResponseEntity<String> clearAllMemory() {
        try {
            chatService.clearAllMemory();
            return ResponseEntity.ok("所有会话的记忆已清除");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("清除记忆失败: " + e.getMessage());
        }
    }
}
