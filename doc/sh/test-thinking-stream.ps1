# GLM-4.6 思考过程流式输出测试脚本 (PowerShell)
# 用于测试增强版流式接口是否能正确区分思考内容和回复内容

$BaseUrl = "http://localhost:8080"
$Endpoint = "/api/chat/stream-enhanced"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " GLM-4.6 思考过程流式输出测试" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 检查服务是否运行
Write-Host "检查服务状态..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/chat/health" -Method GET -TimeoutSec 5
    Write-Host "✓ 服务正常运行" -ForegroundColor Green
} catch {
    Write-Host "错误: 服务未运行，请先启动应用" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 测试 1: 推理问题
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "测试 1: 推理问题（荷花池塘问题）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$reasoningQuestion = "如果一个池塘里的荷花每天增长一倍，30天能覆盖整个池塘，那么覆盖半个池塘需要多少天？"
Write-Host "问题: $reasoningQuestion" -ForegroundColor White
Write-Host ""
Write-Host "发送请求..." -ForegroundColor Yellow
Write-Host ""

$body1 = @{
    message = $reasoningQuestion
    sessionId = $null
} | ConvertTo-Json

try {
    $response1 = Invoke-WebRequest -Uri "$BaseUrl$Endpoint" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"; "Accept"="text/event-stream"} `
        -Body $body1 `
        -TimeoutSec 60

    # 解析并显示 SSE 响应
    $lines = $response1.Content -split "`n"
    foreach ($line in $lines) {
        if ($line.StartsWith("event:")) {
            $eventType = $line.Substring(6).Trim()
            Write-Host "事件类型: $eventType" -ForegroundColor Yellow
        } elseif ($line.StartsWith("data:")) {
            $data = $line.Substring(5).Trim()
            Write-Host "数据: $data" -ForegroundColor White
        }
    }
} catch {
    Write-Host "请求失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# 测试 2: 简单问题
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "测试 2: 简单问题（自我介绍）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$simpleQuestion = "你好，请简单介绍一下你自己"
Write-Host "问题: $simpleQuestion" -ForegroundColor White
Write-Host ""
Write-Host "发送请求..." -ForegroundColor Yellow
Write-Host ""

$body2 = @{
    message = $simpleQuestion
    sessionId = $null
} | ConvertTo-Json

try {
    $response2 = Invoke-WebRequest -Uri "$BaseUrl$Endpoint" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"; "Accept"="text/event-stream"} `
        -Body $body2 `
        -TimeoutSec 60

    # 解析并显示 SSE 响应
    $lines = $response2.Content -split "`n"
    foreach ($line in $lines) {
        if ($line.StartsWith("event:")) {
            $eventType = $line.Substring(6).Trim()
            Write-Host "事件类型: $eventType" -ForegroundColor Yellow
        } elseif ($line.StartsWith("data:")) {
            $data = $line.Substring(5).Trim()
            Write-Host "数据: $data" -ForegroundColor White
        }
    }
} catch {
    Write-Host "请求失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host ""

# 测试 3: 数学问题
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "测试 3: 数学推理（斐波那契）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$mathQuestion = "请计算斐波那契数列第20项的值，并解释计算过程"
Write-Host "问题: $mathQuestion" -ForegroundColor White
Write-Host ""
Write-Host "发送请求..." -ForegroundColor Yellow
Write-Host ""

$body3 = @{
    message = $mathQuestion
    sessionId = $null
} | ConvertTo-Json

try {
    $response3 = Invoke-WebRequest -Uri "$BaseUrl$Endpoint" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"; "Accept"="text/event-stream"} `
        -Body $body3 `
        -TimeoutSec 60

    # 解析并显示 SSE 响应
    $lines = $response3.Content -split "`n"
    foreach ($line in $lines) {
        if ($line.StartsWith("event:")) {
            $eventType = $line.Substring(6).Trim()
            Write-Host "事件类型: $eventType" -ForegroundColor Yellow
        } elseif ($line.StartsWith("data:")) {
            $data = $line.Substring(5).Trim()
            Write-Host "数据: $data" -ForegroundColor White
        }
    }
} catch {
    Write-Host "请求失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "测试完成" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "说明:" -ForegroundColor Yellow
Write-Host "- 'event: reasoning' 表示思考过程内容" -ForegroundColor White
Write-Host "- 'event: content' 表示实际回复内容" -ForegroundColor White
Write-Host "- 'event: done' 表示对话完成（包含 sessionId）" -ForegroundColor White
Write-Host ""
