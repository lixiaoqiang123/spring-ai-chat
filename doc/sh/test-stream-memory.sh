#!/bin/bash

# 流式对话记忆功能测试脚本

echo "========================================="
echo "流式对话记忆功能测试"
echo "========================================="
echo ""

# 测试 1：第一轮对话
echo "测试 1：第一轮对话 - 告诉 AI 名字"
echo "请求: 我的名字叫张三"
echo ""

curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"我的名字叫张三","sessionId":"test-session-123"}' \
  2>/dev/null

echo ""
echo ""
echo "========================================="
echo ""

# 等待一下
sleep 2

# 测试 2：第二轮对话
echo "测试 2：第二轮对话 - 询问名字"
echo "请求: 我的名字是什么？"
echo ""

curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"我的名字是什么？","sessionId":"test-session-123"}' \
  2>/dev/null

echo ""
echo ""
echo "========================================="
echo ""

# 测试 3：第三轮对话
echo "测试 3：第三轮对话 - 询问之前说了什么"
echo "请求: 我刚才说了什么？"
echo ""

curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"我刚才说了什么？","sessionId":"test-session-123"}' \
  2>/dev/null

echo ""
echo ""
echo "========================================="
echo "测试完成！"
echo ""
echo "预期结果："
echo "- 第二轮对话中，AI 应该回复包含'张三'"
echo "- 第三轮对话中，AI 应该能够回忆起之前的对话"
echo ""
echo "如果 AI 无法记住，请检查："
echo "1. 后端日志中的 sessionId 是否一致"
echo "2. Memory Advisor 是否正确配置"
echo "3. ChatMemory Bean 是否正确注入"
echo "========================================="
