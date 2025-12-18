package com.lxq.spring_api_chat.agent.dto;

/**
 * Agent execution step
 * Records each thinking and action step of the Agent
 */
public record AgentStep(
        int stepNumber,         // step number
        StepType type,          // step type
        String content,         // step content
        String toolName,        // tool name if any
        String toolInput,       // tool input if any
        String toolOutput,      // tool output if any
        long timestamp          // timestamp
) {
    /**
     * Step type enum
     */
    public enum StepType {
        THINKING,       // thinking
        PLANNING,       // planning
        TOOL_CALL,      // tool call
        REFLECTION,     // reflection
        FINAL_ANSWER    // final answer
    }

    /**
     * Create thinking step
     */
    public static AgentStep thinking(int stepNumber, String content) {
        return new AgentStep(stepNumber, StepType.THINKING, content,
                null, null, null, System.currentTimeMillis());
    }

    /**
     * Create planning step
     */
    public static AgentStep planning(int stepNumber, String content) {
        return new AgentStep(stepNumber, StepType.PLANNING, content,
                null, null, null, System.currentTimeMillis());
    }

    /**
     * Create tool call step
     */
    public static AgentStep toolCall(int stepNumber, String toolName,
                                      String toolInput, String toolOutput) {
        return new AgentStep(stepNumber, StepType.TOOL_CALL,
                String.format("Call tool: %s", toolName),
                toolName, toolInput, toolOutput, System.currentTimeMillis());
    }

    /**
     * Create reflection step
     */
    public static AgentStep reflection(int stepNumber, String content) {
        return new AgentStep(stepNumber, StepType.REFLECTION, content,
                null, null, null, System.currentTimeMillis());
    }

    /**
     * Create final answer step
     */
    public static AgentStep finalAnswer(int stepNumber, String content) {
        return new AgentStep(stepNumber, StepType.FINAL_ANSWER, content,
                null, null, null, System.currentTimeMillis());
    }
}
