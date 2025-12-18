package com.lxq.spring_api_chat.agent.dto;

import java.util.List;

/**
 * Agent response object
 */
public record AgentResponse(
        String sessionId,           // Session ID
        String finalAnswer,         // Final answer
        List<AgentStep> steps,      // Execution steps list
        boolean success,            // Success flag
        String errorMessage,        // Error message if any
        long totalTime              // Total time in milliseconds
) {
    /**
     * Create success response
     */
    public static AgentResponse success(String sessionId, String finalAnswer,
                                        List<AgentStep> steps, long totalTime) {
        return new AgentResponse(sessionId, finalAnswer, steps,
                true, null, totalTime);
    }

    /**
     * Create failure response
     */
    public static AgentResponse failure(String sessionId, String errorMessage,
                                        List<AgentStep> steps, long totalTime) {
        return new AgentResponse(sessionId, null, steps,
                false, errorMessage, totalTime);
    }
}
