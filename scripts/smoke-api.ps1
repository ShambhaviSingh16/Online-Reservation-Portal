param([string]$BaseUrl='http://127.0.0.1:8080')
$ErrorActionPreference='Stop'
if($BaseUrl -notmatch '^http://(localhost|127\.0\.0\.1):\d+$') { throw 'Only local test servers are permitted.' }
function Api([string]$method,[string]$path,$session,$body=$null,[string]$token='',[int]$expected=200) {
    $args=@{Uri="$BaseUrl$path";Method=$method;WebSession=$session;UseBasicParsing=$true;Headers=@{Accept='application/json'}}
    $session.Headers.Remove('X-CSRF-Token')
    if($token) { $args.Headers['X-CSRF-Token']=$token }
    if($null -ne $body) { $args.Body=($body | ConvertTo-Json -Depth 8); $args.ContentType='application/json' }
    $status=0; $response=$null
    try { $response=Invoke-WebRequest @args; $status=[int]$response.StatusCode }
    catch { if($_.Exception.Response) { $status=[int]$_.Exception.Response.StatusCode } else { throw } }
    if($status -ne $expected) { throw "$method $path expected $expected, received $status" }
    Write-Host "PASS: $method $path ($status)"
    if($response -and $response.Content) { return ($response.Content | ConvertFrom-Json) }
}
function Session { New-Object Microsoft.PowerShell.Commands.WebRequestSession }
function Login($session,[string]$username,[string]$password) {
    $csrf=Api 'GET' '/api/auth/csrf' $session
    Api 'POST' '/api/auth/login' $session @{username=$username;password=$password} $csrf.token
}
$anon=Session
Api 'GET' '/api/bookings/history' $anon $null '' 401 | Out-Null
$health=Api 'GET' '/actuator/health' $anon
if($health.status -ne 'UP') { throw 'Health check failed' }
$docs=Api 'GET' '/v3/api-docs' $anon
if(-not $docs.paths.'/api/bookings') { throw 'Booking OpenAPI documentation missing' }
$passenger=Session; $login=Login $passenger 'demo' 'Demo123!'; $token=$login.csrfToken
Api 'GET' '/api/admin/analytics' $passenger $null '' 403 | Out-Null
Api 'GET' '/api/bookings/history?page=-1' $passenger $null '' 400 | Out-Null
Api 'POST' '/api/bookings' $passenger @{journeyId=-1;requestId=[Guid]::NewGuid().ToString()} $token 400 | Out-Null
Api 'POST' '/api/bookings' $passenger @{journeyId=1;requestId=[Guid]::NewGuid().ToString()} '' 403 | Out-Null
$admin=Session; $adminLogin=Login $admin 'admin' 'Admin123!'; $adminToken=$adminLogin.csrfToken
$number='API'+[Guid]::NewGuid().ToString('N').Substring(0,10)
Api 'POST' '/api/admin/trains' $admin @{trainNumber=$number;trainName='Lifecycle verification';totalSeats=1} $adminToken 201 | Out-Null
$trains=Api 'GET' '/api/admin/trains' $admin
$train=$trains | Where-Object trainNumber -eq $number | Select-Object -First 1
if(-not $train) { throw 'New train not returned' }
$departure=(Get-Date).Date.AddDays(2).AddHours(10)
Api 'POST' '/api/admin/journeys' $admin @{trainId=$train.trainId;departureStationId=1;arrivalStationId=2;departureTime=$departure.ToString('yyyy-MM-ddTHH:mm:ss');arrivalTime=$departure.AddHours(5).ToString('yyyy-MM-ddTHH:mm:ss');fare=1250;availableSeats=1} $adminToken 201 | Out-Null
$journeys=Api 'GET' '/api/admin/journeys' $admin
$journey=$journeys | Where-Object trainId -eq $train.trainId | Select-Object -First 1
$key=[Guid]::NewGuid().ToString()
$first=Api 'POST' '/api/bookings' $passenger @{journeyId=$journey.journeyId;requestId=$key;joinWaitingList=$false} $token 201
if($first.status -ne 'CONFIRMED' -or -not $first.seatNumber) { throw 'Automatic seat allocation failed' }
$retry=Api 'POST' '/api/bookings' $passenger @{journeyId=$journey.journeyId;requestId=$key;joinWaitingList=$false} $token 201
if($retry.ticketId -ne $first.ticketId) { throw 'Repeated request created another ticket' }
$other=Session; $otherCsrf=Api 'GET' '/api/auth/csrf' $other
$username='api'+[Guid]::NewGuid().ToString('N').Substring(0,12)
Api 'POST' '/api/auth/register' $other @{username=$username;password='ApiVerification123!';email="$username@trackease.local";fullName='API verification passenger'} $otherCsrf.token 201 | Out-Null
$otherLogin=Login $other $username 'ApiVerification123!'; $otherToken=$otherLogin.csrfToken
$waiting=Api 'POST' '/api/bookings' $other @{journeyId=$journey.journeyId;requestId=[Guid]::NewGuid().ToString();joinWaitingList=$true} $otherToken 201
if($waiting.status -ne 'WAITLISTED' -or $waiting.waitingPosition -ne 1 -or $waiting.seatNumber) { throw 'Waiting-list state incorrect' }
Api 'GET' "/api/bookings/$($waiting.ticketId)" $passenger $null '' 404 | Out-Null
Api 'DELETE' "/api/bookings/$($waiting.ticketId)" $passenger $null $token 404 | Out-Null
$cancelled=Api 'DELETE' "/api/bookings/$($first.ticketId)" $passenger $null $token
if($cancelled.status -ne 'CANCELLED') { throw 'Cancellation failed' }
$promoted=Api 'GET' "/api/bookings/$($waiting.ticketId)" $other
if($promoted.status -ne 'CONFIRMED' -or $promoted.seatNumber -ne $first.seatNumber) { throw 'FIFO promotion failed' }
Api 'DELETE' "/api/bookings/$($first.ticketId)" $passenger $null $token | Out-Null
$events=Api 'GET' "/api/admin/bookings/$($waiting.ticketId)/events" $admin
if(-not ($events | Where-Object action -eq 'PROMOTED')) { throw 'Promotion event missing' }
$analytics=Api 'GET' '/api/admin/analytics' $admin
if($null -eq $analytics.confirmed_booking_value) { throw 'Analytics missing' }
Api 'GET' '/api/bookings/history?page=0&size=1' $other | Out-Null
Api 'DELETE' "/api/admin/trains/$($train.trainId)" $admin $null $adminToken | Out-Null
Api 'GET' "/api/bookings/$($waiting.ticketId)" $other | Out-Null
Api 'POST' '/api/bookings' $other @{journeyId=$journey.journeyId;requestId=[Guid]::NewGuid().ToString();joinWaitingList=$true} $otherToken 409 | Out-Null
Api 'DELETE' "/api/bookings/$($waiting.ticketId)" $other $null $otherToken | Out-Null
Api 'POST' '/api/auth/logout' $other @{} $otherToken | Out-Null
Api 'GET' '/api/auth/me' $other $null '' 401 | Out-Null
Write-Host 'All API lifecycle checks passed. The test train was archived; booking history was retained.'
