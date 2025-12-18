package com.lxq.spring_api_chat.agent.tool;

import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 计算器工具
 * 用于执行数学计算
 */
@Component
public class CalculatorTool implements AgentTool {

    private final ScriptEngine engine;

    public CalculatorTool() {
        this.engine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算。支持基本的算术运算(+, -, *, /)和数学函数(Math.sqrt, Math.pow等)。";
    }

    @Override
    public String getParameterDescription() {
        return "expression: 要计算的数学表达式，例如: '2 + 2', '10 * 5', 'Math.sqrt(16)'";
    }

    @Override
    public String execute(String input) {
        try {
            // 安全检查：只允许数学表达式
            if (input.contains("import") || input.contains("System") || input.contains("Runtime")) {
                return "错误：不允许执行系统命令";
            }

            Object result = engine.eval(input);
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }
}
