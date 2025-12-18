package com.lxq.spring_api_chat.agent.service;

import com.lxq.spring_api_chat.agent.dto.AgentRequest;
import com.lxq.spring_api_chat.agent.dto.AgentResponse;
import com.lxq.spring_api_chat.agent.dto.AgentStep;
import com.lxq.spring_api_chat.agent.tool.AgentTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent Service
 * Implements an intelligent agent with thinking, planning and reflection capabilities
 */
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final Map<String, AgentTool> tools;

    /**
     * System prompt - defines Agent behavior pattern
     */
    private static final String SYSTEM_PROMPT = "You are an intelligent assistant with thinking and planning capabilities.\n\n" +
            "Your workflow:\n" +
            "1. **Thinking**: Analyze user task and understand requirements\n" +
            "2. **Planning**: Develop solution steps\n" +
            "3. **Action**: Use available tools to execute tasks\n" +
            "4. **Reflection**: Evaluate if results meet requirements\n\n" +
            "Available tools:\n%s\n\n" +
            "Important rules:\n" +
            "- Must think before acting\n" +
            "- To use a tool, clearly state: USE_TOOL: [tool_name] [parameters]\n" +
            "- Can only use one tool at a time\n" +
            "- When task is complete, state: FINAL_ANSWER: [answer]\n" +
            "- If encountering problems, reflect and adjust strategy\n";

    public AgentService(ChatModel chatModel, List<AgentTool> toolList) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.tools = toolList.stream()
                .collect(Collectors.toMap(AgentTool::getName, Function.identity()));
    }

    /**
     * Execute Agent task
     */
    public AgentResponse execute(AgentRequest request) {
        long startTime = System.currentTimeMillis();
        List<AgentStep> steps = new ArrayList<>();
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();

        try {
            // Build system prompt
            String systemPrompt = buildSystemPrompt();

            // Agent execution loop
            String currentTask = request.task();
            int stepNumber = 1;

            while (stepNumber <= request.maxSteps()) {
                // 1. Thinking phase
                String thinking = think(currentTask, steps);
                steps.add(AgentStep.thinking(stepNumber++, thinking));

                // Check if final answer is reached
                if (thinking.contains("FINAL_ANSWER:")) {
                    String finalAnswer = extractFinalAnswer(thinking);
                    steps.add(AgentStep.finalAnswer(stepNumber, finalAnswer));
                    long totalTime = System.currentTimeMillis() - startTime;
                    return AgentResponse.success(sessionId, finalAnswer, steps, totalTime);
                }

                // 2. Check if tool is needed
                if (thinking.contains("USE_TOOL:")) {
                    ToolCall toolCall = parseToolCall(thinking);
                    if (toolCall != null && tools.containsKey(toolCall.toolName)) {
                        // Execute tool
                        AgentTool tool = tools.get(toolCall.toolName);
                        String toolOutput = tool.execute(toolCall.input);
                        steps.add(AgentStep.toolCall(stepNumber++, toolCall.toolName,
                                toolCall.input, toolOutput));

                        // 3. Reflection phase - evaluate tool output
                        String reflection = reflect(currentTask, toolOutput, steps);
                        steps.add(AgentStep.reflection(stepNumber++, reflection));

                        // Update current task context
                        currentTask = String.format("%s\nTool output: %s\nReflection: %s",
                                request.task(), toolOutput, reflection);
                    }
                } else {
                    // If no tool call and no final answer, continue thinking
                    currentTask = String.format("%s\nPrevious thinking: %s", request.task(), thinking);
                }

                // Prevent infinite loop
                if (stepNumber > request.maxSteps()) {
                    break;
                }
            }

            // If max steps reached without completion
            String finalThinking = think(currentTask + "\nPlease provide final answer", steps);
            String finalAnswer = extractContent(finalThinking);
            steps.add(AgentStep.finalAnswer(stepNumber, finalAnswer));

            long totalTime = System.currentTimeMillis() - startTime;
            return AgentResponse.success(sessionId, finalAnswer, steps, totalTime);

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            return AgentResponse.failure(sessionId, e.getMessage(), steps, totalTime);
        }
    }

    /**
     * Thinking phase - use AI model for reasoning
     */
    private String think(String task, List<AgentStep> previousSteps) {
        String context = buildContext(previousSteps);
        String prompt = String.format("Task: %s\n\nHistory:\n%s\n\nPlease think and decide next action.",
                task, context);

        return chatClient.prompt()
                .system(buildSystemPrompt())
                .user(prompt)
                .call()
                .content();
    }

    /**
     * Reflection phase - evaluate execution results
     */
    private String reflect(String originalTask, String toolOutput, List<AgentStep> steps) {
        String prompt = String.format("Original task: %s\nTool output: %s\n\n" +
                        "Please reflect:\n" +
                        "1. Is the tool output helpful for completing the task?\n" +
                        "2. Do we need further actions?\n" +
                        "3. If task is completed, provide final answer",
                originalTask, toolOutput);

        return chatClient.prompt()
                .system("You are an assistant good at reflection and evaluation")
                .user(prompt)
                .call()
                .content();
    }

    /**
     * Build system prompt
     */
    private String buildSystemPrompt() {
        StringBuilder toolsDesc = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            toolsDesc.append(String.format("- %s: %s\n  Parameters: %s\n",
                    tool.getName(), tool.getDescription(), tool.getParameterDescription()));
        }
        return String.format(SYSTEM_PROMPT, toolsDesc.toString());
    }

    /**
     * Build history context
     */
    private String buildContext(List<AgentStep> steps) {
        if (steps.isEmpty()) {
            return "None";
        }

        StringBuilder context = new StringBuilder();
        for (AgentStep step : steps) {
            context.append(String.format("Step%d [%s]: %s\n",
                    step.stepNumber(), step.type(), step.content()));
            if (step.toolOutput() != null) {
                context.append(String.format("  Tool output: %s\n", step.toolOutput()));
            }
        }
        return context.toString();
    }

    /**
     * Parse tool call
     */
    private ToolCall parseToolCall(String thinking) {
        try {
            int startIdx = thinking.indexOf("USE_TOOL:");
            if (startIdx == -1) return null;

            String toolPart = thinking.substring(startIdx + 9).trim();
            String[] parts = toolPart.split("\\s+", 2);

            if (parts.length >= 1) {
                String toolName = parts[0].trim();
                String input = parts.length > 1 ? parts[1].trim() : "";
                return new ToolCall(toolName, input);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse tool call: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract final answer
     */
    private String extractFinalAnswer(String thinking) {
        int idx = thinking.indexOf("FINAL_ANSWER:");
        if (idx != -1) {
            return thinking.substring(idx + 13).trim();
        }
        return thinking;
    }

    /**
     * Extract content
     */
    private String extractContent(String text) {
        if (text.contains("FINAL_ANSWER:")) {
            return extractFinalAnswer(text);
        }
        return text;
    }

    /**
     * Tool call record
     */
    private record ToolCall(String toolName, String input) {}
}
