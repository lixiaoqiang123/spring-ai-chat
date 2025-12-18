#!/bin/bash
########################################
# Spring Boot 应用启动脚本（支持 Profile 切换）
########################################

# 默认使用 zhipu profile
PROFILE=${1:-zhipu}

echo "========================================"
echo "启动 Spring Boot 应用"
echo "Profile: $PROFILE"
echo "========================================"
echo

# 运行应用
mvn spring-boot:run -P"$PROFILE"
