package com.lxq.spring_api_chat.agent.controller;

import com.lxq.spring_api_chat.agent.dto.AgentRequest;
import com.lxq.spring_api_chat.agent.dto.AgentResponse;
import com.lxq.spring_api_chat.agent.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 控制器
 * 提供 Agent 相关的 REST API
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 执行 Agent 任务
     *
     * @param request Agent 请求
     * @return Agent 响应，包含执行步骤和最终答案
     */
    @PostMapping("/execute")
    public ResponseEntity<AgentResponse> execute(@RequestBody AgentRequest request) {
        try {
            // 验证请求
            if (request.task() == null || request.task().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 执行 Agent 任务
            AgentResponse response = agentService.execute(request);

            // 根据执行结果返回相应的 HTTP 状态码
            if (response.success()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }
        } catch (Exception e) {
            // 处理异常
            AgentResponse errorResponse = AgentResponse.failure(
                    request.sessionId(),
                    "执行失败: " + e.getMessage(),
                    null,
                    0
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 简单的 Agent 任务接口 - 使用 GET 请求
     *
     * @param task 任务描述
     * @return Agent 响应
     */
    @GetMapping("/ask")
    public ResponseEntity<AgentResponse> ask(@RequestParam String task) {
        AgentRequest request = new AgentRequest(task, null,null);
        return execute(request);
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Agent service is running");
    }
}
