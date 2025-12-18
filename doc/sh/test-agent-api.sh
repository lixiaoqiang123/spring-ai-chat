#!/bin/bash

# Agent API 测试脚本
# 用于测试 /api/agent/execute 接口

BASE_URL="http://localhost:8080"

echo "========================================="
echo "Agent API 测试脚本"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试1: 健康检查
echo -e "${YELLOW}测试1: 健康检查${NC}"
echo "GET $BASE_URL/api/agent/health"
curl -s "$BASE_URL/api/agent/health"
echo -e "\n"

# 测试2: 简单计算任务
echo -e "${YELLOW}测试2: 计算任务 (25 * 4 + 10)${NC}"
echo "POST $BASE_URL/api/agent/execute"
curl -s -X POST "$BASE_URL/api/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "task": "计算 25 * 4 + 10 的结果",
    "sessionId": "test-session-1",
    "maxSteps": 10
  }' | jq '.'
echo -e "\n"

# 测试3: 天气查询任务
echo -e "${YELLOW}测试3: 天气查询任务${NC}"
echo "POST $BASE_URL/api/agent/execute"
curl -s -X POST "$BASE_URL/api/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "task": "查询北京的天气情况",
    "sessionId": "test-session-2",
    "maxSteps": 10
  }' | jq '.'
echo -e "\n"

# 测试4: 搜索任务
echo -e "${YELLOW}测试4: 搜索任务${NC}"
echo "POST $BASE_URL/api/agent/execute"
curl -s -X POST "$BASE_URL/api/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "task": "搜索 Spring AI 的相关信息",
    "sessionId": "test-session-3",
    "maxSteps": 10
  }' | jq '.'
echo -e "\n"

# 测试5: 复杂任务（多工具协作）
echo -e "${YELLOW}测试5: 复杂任务（多工具协作）${NC}"
echo "POST $BASE_URL/api/agent/execute"
curl -s -X POST "$BASE_URL/api/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "task": "先查询上海的天气，然后计算如果温度是18度，转换成华氏度是多少",
    "sessionId": "test-session-4",
    "maxSteps": 15
  }' | jq '.'
echo -e "\n"

# 测试6: 使用 GET 接口的简单查询
echo -e "${YELLOW}测试6: GET 接口简单查询${NC}"
echo "GET $BASE_URL/api/agent/ask?task=你好"
curl -s "$BASE_URL/api/agent/ask?task=你好" | jq '.'
echo -e "\n"

# 测试7: 错误处理 - 空任务
echo -e "${YELLOW}测试7: 错误处理 - 空任务${NC}"
echo "POST $BASE_URL/api/agent/execute"
curl -s -X POST "$BASE_URL/api/agent/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "task": "",
    "sessionId": "test-session-5",
    "maxSteps": 10
  }' | jq '.'
echo -e "\n"

echo "========================================="
echo "测试完成"
echo "========================================="
