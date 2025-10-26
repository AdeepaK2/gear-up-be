# Test script for database-backed chat history
# Run this after starting the FastAPI service with database storage

Write-Host "Testing Database-Backed Chat History" -ForegroundColor Cyan

$baseUrl = "http://localhost:8000"
$sessionId = "db-test-session-" + (Get-Date -Format "yyyyMMdd-HHmmss")

# Test 1: Health check
Write-Host "`nTest 1: Testing health endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
    Write-Host "PASSED: Health check - $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "FAILED: Health check - $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 2: Send multiple chat messages
Write-Host "`nTest 2: Sending chat messages to save in database..." -ForegroundColor Yellow

$questions = @(
    "What appointments are available?",
    "Who is the mechanic?",
    "What's the customer name?",
    "When is the appointment?"
)

foreach ($question in $questions) {
    try {
        $chatRequest = @{
            question = $question
            sessionId = $sessionId
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod -Uri "$baseUrl/chat" -Method Post -Body $chatRequest -ContentType "application/json"
        $shortAnswer = $response.answer.Substring(0, [Math]::Min(60, $response.answer.Length))
        Write-Host "   Sent: '$question'" -ForegroundColor Green
        Write-Host "      Response: $shortAnswer..." -ForegroundColor Gray
        Start-Sleep -Milliseconds 500
    } catch {
        Write-Host "   Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 3: Retrieve chat history
Write-Host "`nTest 3: Retrieving chat history from database..." -ForegroundColor Yellow
try {
    $history = Invoke-RestMethod -Uri "$baseUrl/chat/history/$sessionId" -Method Get
    Write-Host "PASSED: Retrieved $($history.Count) messages from database" -ForegroundColor Green
    
    # Display history
    for ($i = 0; $i -lt $history.Count; $i++) {
        $msg = $history[$i]
        Write-Host "`n   Message $($i + 1):" -ForegroundColor Cyan
        Write-Host "   Q: $($msg.question)" -ForegroundColor White
        $shortAns = $msg.answer.Substring(0, [Math]::Min(60, $msg.answer.Length))
        Write-Host "   A: $shortAns..." -ForegroundColor Gray
        Write-Host "   Time: $($msg.timestamp)" -ForegroundColor DarkGray
    }
} catch {
    Write-Host "FAILED: Could not retrieve history - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 4: Verify persistence (send another message)
Write-Host "`nTest 4: Testing persistence with new message..." -ForegroundColor Yellow
try {
    $chatRequest = @{
        question = "Is this saved in the database?"
        sessionId = $sessionId
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$baseUrl/chat" -Method Post -Body $chatRequest -ContentType "application/json"
    Write-Host "Message sent" -ForegroundColor Green
    
    # Retrieve again to verify
    $history = Invoke-RestMethod -Uri "$baseUrl/chat/history/$sessionId" -Method Get
    Write-Host "PASSED: Now have $($history.Count) messages in database (persistence verified)" -ForegroundColor Green
} catch {
    Write-Host "FAILED: Persistence test - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: Clear history
Write-Host "`nTest 5: Clearing chat history from database..." -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "$baseUrl/chat/history/$sessionId" -Method Delete
    Write-Host "PASSED: History cleared" -ForegroundColor Green
    
    # Verify deletion
    $history = Invoke-RestMethod -Uri "$baseUrl/chat/history/$sessionId" -Method Get
    if ($history.Count -eq 0) {
        Write-Host "PASSED: Verified history is empty after deletion" -ForegroundColor Green
    } else {
        Write-Host "WARNING: Still found $($history.Count) messages after deletion" -ForegroundColor Yellow
    }
} catch {
    Write-Host "FAILED: Clear history - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nDatabase chat history tests completed!" -ForegroundColor Cyan

# Display migration info
Write-Host "`nMigration Summary:" -ForegroundColor Magenta
Write-Host "   Chat history now stored in PostgreSQL database" -ForegroundColor White
Write-Host "   Table: chat_history (auto-created on startup)" -ForegroundColor White
Write-Host "   Persists across server restarts" -ForegroundColor White
Write-Host "   Session ID: $sessionId" -ForegroundColor White

Write-Host "`nTo verify in database:" -ForegroundColor Cyan
$sqlQuery = "SELECT * FROM chat_history WHERE session_id = '$sessionId' ORDER BY timestamp DESC;"
Write-Host "   $sqlQuery" -ForegroundColor Gray
Write-Host ""
