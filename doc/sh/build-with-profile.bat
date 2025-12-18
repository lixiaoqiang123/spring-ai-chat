@echo off
REM ========================================
REM Spring Boot 应用构建脚本（支持 Profile 切换）
REM ========================================

setlocal

REM 默认使用 zhipu profile
set PROFILE=zhipu

REM 如果提供了参数，使用参数作为 profile
if not "%1"=="" set PROFILE=%1

echo ========================================
echo 构建 Spring Boot 应用
echo Profile: %PROFILE%
echo ========================================
echo.

REM 清理并打包
mvn clean package -P%PROFILE% -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo 构建成功！
    echo JAR 文件位置: target\spring-api-chat-0.0.1-SNAPSHOT.jar
    echo ========================================
    echo.
    echo 运行应用:
    echo java -jar target\spring-api-chat-0.0.1-SNAPSHOT.jar
) else (
    echo.
    echo ========================================
    echo 构建失败！
    echo ========================================
)

endlocal
