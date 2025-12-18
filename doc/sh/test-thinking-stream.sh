#!/bin/bash

# GLM-4.6 思考过程流式输出测试脚本
# 用于测试增强版流式接口是否能正确区分思考内容和回复内容

BASE_URL="http://localhost:8080"
ENDPOINT="/api/chat/stream-enhanced"

echo "=========================================="
echo " GLM-4.6 思考过程流式输出测试"
echo "=========================================="
echo ""

# 检查服务是否运行
echo "检查服务状态..."
curl -s "${BASE_URL}/api/chat/health" > /dev/null
if [ $? -ne 0 ]; then
    echo "错误: 服务未运行，请先启动应用"
    exit 1
fi
echo "✓ 服务正常运行"
echo ""

# 测试 1: 推理问题
echo "=========================================="
echo "测试 1: 推理问题（荷花池塘问题）"
echo "=========================================="
echo ""

REASONING_QUESTION="如果一个池塘里的荷花每天增长一倍，30天能覆盖整个池塘，那么覆盖半个池塘需要多少天？"

echo "问题: ${REASONING_QUESTION}"
echo ""
echo "发送请求..."
echo ""

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{
    \"message\": \"${REASONING_QUESTION}\",
    \"sessionId\": null
  }" \
  -N --no-buffer

echo ""
echo ""

# 测试 2: 简单问题
echo "=========================================="
echo "测试 2: 简单问题（自我介绍）"
echo "=========================================="
echo ""

SIMPLE_QUESTION="你好，请简单介绍一下你自己"

echo "问题: ${SIMPLE_QUESTION}"
echo ""
echo "发送请求..."
echo ""

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{
    \"message\": \"${SIMPLE_QUESTION}\",
    \"sessionId\": null
  }" \
  -N --no-buffer

echo ""
echo ""

# 测试 3: 数学问题
echo "=========================================="
echo "测试 3: 数学推理（斐波那契）"
echo "=========================================="
echo ""

MATH_QUESTION="请计算斐波那契数列第20项的值，并解释计算过程"

echo "问题: ${MATH_QUESTION}"
echo ""
echo "发送请求..."
echo ""

curl -X POST "${BASE_URL}${ENDPOINT}" \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d "{
    \"message\": \"${MATH_QUESTION}\",
    \"sessionId\": null
  }" \
  -N --no-buffer

echo ""
echo ""
echo "=========================================="
echo "测试完成"
echo "=========================================="
echo ""
echo "说明:"
echo "- 'event: reasoning' 表示思考过程内容"
echo "- 'event: content' 表示实际回复内容"
echo "- 'event: done' 表示对话完成（包含 sessionId）"
echo ""
