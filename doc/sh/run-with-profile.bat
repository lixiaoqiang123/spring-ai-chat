@echo off
REM ========================================
REM Spring Boot 应用启动脚本（支持 Profile 切换）
REM ========================================

setlocal

REM 默认使用 zhipu profile
set PROFILE=zhipu

REM 如果提供了参数，使用参数作为 profile
if not "%1"=="" set PROFILE=%1

echo ========================================
echo 启动 Spring Boot 应用
echo Profile: %PROFILE%
echo ========================================
echo.

REM 运行应用
mvn spring-boot:run -P%PROFILE%

endlocal
