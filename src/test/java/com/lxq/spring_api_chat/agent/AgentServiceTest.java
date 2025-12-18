package com.lxq.spring_api_chat.agent;

import com.lxq.spring_api_chat.agent.dto.AgentRequest;
import com.lxq.spring_api_chat.agent.dto.AgentResponse;
import com.lxq.spring_api_chat.agent.service.AgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 服务测试
 */
@SpringBootTest
class AgentServiceTest {

    @Autowired
    private AgentService agentService;

    @Test
    void testCalculatorTool() {
        // 测试计算器工具
        AgentRequest request = new AgentRequest(
                "计算 25 * 4 + 10 的结果",
                null,
                10
        );

        AgentResponse response = agentService.execute(request);

        assertNotNull(response);
        assertTrue(response.success(), "Agent 应该成功执行");
        assertNotNull(response.finalAnswer(), "应该有最终答案");
        assertFalse(response.steps().isEmpty(), "应该有执行步骤");

        System.out.println("=== 计算器测试 ===");
        System.out.println("最终答案: " + response.finalAnswer());
        System.out.println("执行步骤数: " + response.steps().size());
        System.out.println("总耗时: " + response.totalTime() + "ms");
        response.steps().forEach(step ->
                System.out.println(String.format("步骤%d [%s]: %s",
                        step.stepNumber(), step.type(), step.content()))
        );
    }

    @Test
    void testWeatherTool() {
        // 测试天气工具
        AgentRequest request = new AgentRequest(
                "查询北京的天气情况",
                null,
                10
        );

        AgentResponse response = agentService.execute(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.finalAnswer());

        System.out.println("\n=== 天气查询测试 ===");
        System.out.println("最终答案: " + response.finalAnswer());
        System.out.println("执行步骤数: " + response.steps().size());
        response.steps().forEach(step ->
                System.out.println(String.format("步骤%d [%s]: %s",
                        step.stepNumber(), step.type(), step.content()))
        );
    }

    @Test
    void testSearchTool() {
        // 测试搜索工具
        AgentRequest request = new AgentRequest(
                "搜索 Spring AI 的相关信息",
                null,
                10
        );

        AgentResponse response = agentService.execute(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.finalAnswer());

        System.out.println("\n=== 搜索测试 ===");
        System.out.println("最终答案: " + response.finalAnswer());
        System.out.println("执行步骤数: " + response.steps().size());
        response.steps().forEach(step ->
                System.out.println(String.format("步骤%d [%s]: %s",
                        step.stepNumber(), step.type(), step.content()))
        );
    }

    @Test
    void testComplexTask() {
        // 测试复杂任务 - 需要多个工具协作
        AgentRequest request = new AgentRequest(
                "先查询上海的天气，然后计算如果温度是18度，转换成华氏度是多少",
                null,
                15
        );

        AgentResponse response = agentService.execute(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.finalAnswer());

        System.out.println("\n=== 复杂任务测试 ===");
        System.out.println("最终答案: " + response.finalAnswer());
        System.out.println("执行步骤数: " + response.steps().size());
        System.out.println("总耗时: " + response.totalTime() + "ms");
        response.steps().forEach(step -> {
            System.out.println(String.format("\n步骤%d [%s]: %s",
                    step.stepNumber(), step.type(), step.content()));
            if (step.toolName() != null) {
                System.out.println("  工具: " + step.toolName());
                System.out.println("  输入: " + step.toolInput());
                System.out.println("  输出: " + step.toolOutput());
            }
        });
    }

    @Test
    void testMaxStepsLimit() {
        // 测试最大步数限制
        AgentRequest request = new AgentRequest(
                "这是一个简单的问候",
                null,
                3  // 限制最多3步
        );

        AgentResponse response = agentService.execute(request);

        assertNotNull(response);
        assertTrue(response.steps().size() <= 3 * 2, "步骤数应该在限制范围内");

        System.out.println("\n=== 最大步数限制测试 ===");
        System.out.println("最终答案: " + response.finalAnswer());
        System.out.println("执行步骤数: " + response.steps().size());
    }
}
