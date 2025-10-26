# PowerShell script to test the RAG Chatbot Service

Write-Host "`n===========================================================" -ForegroundColor Cyan
Write-Host "RAG Chatbot Service Test" -ForegroundColor Cyan
Write-Host "===========================================================`n" -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8000/health" -Method Get
    Write-Host "Status: " -NoNewline -ForegroundColor Green
    Write-Host $health.status
    Write-Host "Gemini Available: " -NoNewline -ForegroundColor Green
    Write-Host $health.gemini
    Write-Host "Vector DB Available: " -NoNewline -ForegroundColor Green
    Write-Host $health.vector_db
} catch {
    Write-Host "ERROR: Service not responding. Make sure it's running on port 8000" -ForegroundColor Red
    Write-Host "Start with: cd chatbot-service; py -3.11 -m uvicorn main:app --port 8000" -ForegroundColor Yellow
    exit
}

# Test 2: Simple Chat Question
Write-Host "`nTest 2: Chat - What appointments are available?" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
$body1 = @{
    question = "What appointments are available?"
    sessionId = "test_conv_1"
} | ConvertTo-Json

try {
    $response1 = Invoke-RestMethod -Uri "http://localhost:8000/chat" -Method Post -Body $body1 -ContentType "application/json"
    Write-Host "Question: " -NoNewline -ForegroundColor Cyan
    Write-Host "What appointments are available?"
    Write-Host "Answer: " -NoNewline -ForegroundColor Green
    Write-Host $response1.answer
    if ($response1.confidence) {
        Write-Host "`nMetadata:" -ForegroundColor Gray
        Write-Host "  Confidence: $($response1.confidence)" -ForegroundColor Gray
        Write-Host "  Sources: $($response1.sources -join ', ')" -ForegroundColor Gray
        Write-Host "  Processing Time: $($response1.processing_time_ms)ms" -ForegroundColor Gray
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Pending Appointments
Write-Host "`nTest 3: Chat - Show pending appointments" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
$body2 = @{
    question = "Show me all pending appointments"
    sessionId = "test_conv_1"
} | ConvertTo-Json

try {
    $response2 = Invoke-RestMethod -Uri "http://localhost:8000/chat" -Method Post -Body $body2 -ContentType "application/json"
    Write-Host "Question: " -NoNewline -ForegroundColor Cyan
    Write-Host "Show me all pending appointments"
    Write-Host "Answer: " -NoNewline -ForegroundColor Green
    Write-Host $response2.answer
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Time Slots
Write-Host "`nTest 4: Chat - Available time slots" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
$body3 = @{
    question = "What time slots are available for appointments?"
    sessionId = "test_conv_1"
} | ConvertTo-Json

try {
    $response3 = Invoke-RestMethod -Uri "http://localhost:8000/chat" -Method Post -Body $body3 -ContentType "application/json"
    Write-Host "Question: " -NoNewline -ForegroundColor Cyan
    Write-Host "What time slots are available for appointments?"
    Write-Host "Answer: " -NoNewline -ForegroundColor Green
    Write-Host $response3.answer
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Vehicle Services
Write-Host "`nTest 5: Chat - Vehicle maintenance" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
$body4 = @{
    question = "Tell me about vehicle maintenance appointments"
    sessionId = "test_conv_1"
} | ConvertTo-Json

try {
    $response4 = Invoke-RestMethod -Uri "http://localhost:8000/chat" -Method Post -Body $body4 -ContentType "application/json"
    Write-Host "Question: " -NoNewline -ForegroundColor Cyan
    Write-Host "Tell me about vehicle maintenance appointments"
    Write-Host "Answer: " -NoNewline -ForegroundColor Green
    Write-Host $response4.answer
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Chat History
Write-Host "`nTest 6: Get Chat History" -ForegroundColor Yellow
Write-Host "------------------------------------------------------------" -ForegroundColor Gray
try {
    $history = Invoke-RestMethod -Uri "http://localhost:8000/chat/history/test_conv_1?limit=5" -Method Get
    Write-Host "Retrieved $($history.Count) chat messages" -ForegroundColor Green
    if ($history.Count -gt 0) {
        Write-Host "Latest message:" -ForegroundColor Gray
        Write-Host "  Question: $($history[0].question)" -ForegroundColor Gray
        Write-Host "  Answer: $($history[0].answer.Substring(0, [Math]::Min(100, $history[0].answer.Length)))..." -ForegroundColor Gray
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n===========================================================" -ForegroundColor Cyan
Write-Host "Testing Complete!" -ForegroundColor Green
Write-Host "===========================================================`n" -ForegroundColor Cyan

Write-Host "To view the API documentation, open: http://localhost:8000/docs" -ForegroundColor Yellow
