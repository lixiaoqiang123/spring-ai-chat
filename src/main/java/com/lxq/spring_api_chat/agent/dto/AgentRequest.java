package com.lxq.spring_api_chat.agent.dto;

/**
 * Agent request object
 * Using Java 21 Record feature
 */
public record AgentRequest(
        String task,        // User task description
        String sessionId,   // Session ID
        Integer maxSteps    // Max execution steps (optional, default 10)
) {
    /**
     * Compact constructor with default values
     */
    public AgentRequest {
        if (maxSteps == null || maxSteps <= 0) {
            maxSteps = 10;
        }
    }
}
