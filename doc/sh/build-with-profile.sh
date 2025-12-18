#!/bin/bash
########################################
# Spring Boot 应用构建脚本（支持 Profile 切换）
########################################

# 默认使用 zhipu profile
PROFILE=${1:-zhipu}

echo "========================================"
echo "构建 Spring Boot 应用"
echo "Profile: $PROFILE"
echo "========================================"
echo

# 清理并打包
mvn clean package -P"$PROFILE" -DskipTests

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "构建成功！"
    echo "JAR 文件位置: target/spring-api-chat-0.0.1-SNAPSHOT.jar"
    echo "========================================"
    echo
    echo "运行应用:"
    echo "java -jar target/spring-api-chat-0.0.1-SNAPSHOT.jar"
else
    echo
    echo "========================================"
    echo "构建失败！"
    echo "========================================"
    exit 1
fi
