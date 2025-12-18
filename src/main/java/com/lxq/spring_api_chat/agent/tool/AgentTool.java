package com.lxq.spring_api_chat.agent.tool;

/**
 * Agent 工具接口
 * 所有 Agent 可用的工具都需要实现此接口
 */
public interface AgentTool {

    /**
     * 获取工具名称
     *
     * @return 工具名称
     */
    String getName();

    /**
     * 获取工具描述
     * 用于 AI 模型理解工具的功能
     *
     * @return 工具描述
     */
    String getDescription();

    /**
     * 执行工具
     *
     * @param input 工具输入参数
     * @return 工具执行结果
     */
    String execute(String input);

    /**
     * 获取工具参数说明
     *
     * @return 参数说明
     */
    String getParameterDescription();
}
