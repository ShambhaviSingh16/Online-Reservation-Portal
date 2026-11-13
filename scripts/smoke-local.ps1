# Exercises the isolated local demo database; leaves a demo ticket for inspection.
param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
if ($BaseUrl -notmatch '^http://(127\.0\.0\.1|localhost):\d+$') {
    throw 'This smoke check is restricted to a local server.'
}
function Check($response, [string]$text, [string]$label) {
    if ($response.StatusCode -ne 200 -or $response.Content -notmatch $text) { throw "Failed: $label" }
    Write-Host "PASS: $label"
}
function PostForm([string]$url, [hashtable]$body, $session) {
    $form = Invoke-WebRequest -UseBasicParsing "$BaseUrl/login.jsp" -WebSession $session
    $token = [regex]::Match($form.Content, 'name="csrfToken" value="([^"]+)"')
    if (-not $token.Success) { throw 'CSRF token missing from login form' }
    $body['csrfToken'] = $token.Groups[1].Value
    Invoke-WebRequest -UseBasicParsing $url -Method Post -Body $body -WebSession $session
}
Check (Invoke-WebRequest -UseBasicParsing "$BaseUrl/") 'TrackEase' 'Home page'
$passenger = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$response = PostForm "$BaseUrl/login" @{username='demo';password='Demo123!'} $passenger
Check $response 'Demo Passenger' 'Passenger login and dashboard'
Check (Invoke-WebRequest -UseBasicParsing "$BaseUrl/user/bookTicket.jsp" -WebSession $passenger) 'New Delhi' 'Station list from PostgreSQL'
$date = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
$response = PostForm "$BaseUrl/user/searchJourney" @{departureStation='1';arrivalStation='2';travelDate=$date} $passenger
Check $response 'TrackEase Demo Express' 'Journey search'
$demoForm = [regex]::Matches($response.Content,'(?s)<form\b[^>]*>.*?</form>') |
    Where-Object { $_.Value -match 'TrackEase Demo Express' } | Select-Object -First 1
if (-not $demoForm) { throw 'No demo journey booking form returned.' }
$match = [regex]::Match($demoForm.Value,'name="journeyId" value="(\d+)"')
if (-not $match.Success) { throw 'No bookable journey returned.' }
$requestMatch = [regex]::Match($demoForm.Value,'name="bookingRequestId" value="([^"]+)"')
if (-not $requestMatch.Success) { throw 'Booking request identifier missing' }
$requestId = $requestMatch.Groups[1].Value
if ($response.Content -match 'name="(seatNumber|coachNumber)"') { throw 'Manual seat fields still present' }
$response = PostForm "$BaseUrl/user/bookTicket" @{journeyId=$match.Groups[1].Value;bookingRequestId=$requestId} $passenger
$assigned = [regex]::Match($response.Content, 'fa-chair[^<]*</i>\s*(\d+)\s*\((S\d+)\)')
if (-not $assigned.Success) { throw 'No automatically assigned seat shown' }
$seat = $assigned.Groups[1].Value
$coach = $assigned.Groups[2].Value
$seatPattern = [regex]::Escape("$seat ($coach)")
Check $response $seatPattern 'Automatic allocation and confirmation'
$confirmationUrl = $response.BaseResponse.ResponseUri.AbsoluteUri
Check (Invoke-WebRequest -UseBasicParsing "$BaseUrl/user/tickets" -WebSession $passenger) ([regex]::Escape("$seat-$coach")) 'Ticket history'
$admin = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Check (PostForm "$BaseUrl/login" @{username='admin';password='Admin123!'} $admin) 'Admin Dashboard' 'Administrator login'
foreach ($path in @('/admin/train','/admin/station','/admin/journey','/admin/tickets')) {
    Check (Invoke-WebRequest -UseBasicParsing "$BaseUrl$path" -WebSession $admin) '<html' $path
}
function CheckDenied([string]$url, $session, [int]$status, [string]$label) {
    $actual = 0
    try { $result = Invoke-WebRequest -UseBasicParsing $url -WebSession $session; $actual = [int]$result.StatusCode }
    catch {
        if ($_.Exception.Response) { $actual = [int]$_.Exception.Response.StatusCode }
        else { throw }
    }
    if ($actual -ne $status) { throw "Failed: $label (expected $status, received $actual)" }
    Write-Host "PASS: $label"
}
CheckDenied "$BaseUrl/admin/adminDashboard.jsp" $passenger 403 'Passenger cannot open admin JSP'
CheckDenied "$BaseUrl/admin/tickets" $passenger 403 'Passenger cannot open admin servlet'
$anonymous = New-Object Microsoft.PowerShell.Commands.WebRequestSession
Check (Invoke-WebRequest -UseBasicParsing $confirmationUrl -WebSession $anonymous) '<title>.*Login' 'Anonymous ticket access redirects to login'
$other = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$username = 'check' + [Guid]::NewGuid().ToString('N').Substring(0,12)
Check (PostForm "$BaseUrl/register" @{username=$username;password='LocalCheck123!';email="$username@trackease.local";fullName='Ownership Test'} $other) 'Registration successful' 'Register second passenger'
Check (PostForm "$BaseUrl/login" @{username=$username;password='LocalCheck123!'} $other) 'Ownership Test' 'Second passenger login'
CheckDenied $confirmationUrl $other 404 'Another passenger cannot view the ticket'
Check (Invoke-WebRequest -UseBasicParsing $confirmationUrl -WebSession $admin) $seatPattern 'Administrator can inspect ticket'
$retry = PostForm "$BaseUrl/user/bookTicket" @{journeyId=$match.Groups[1].Value;bookingRequestId=$requestId} $passenger
if ($retry.BaseResponse.ResponseUri.AbsoluteUri -ne $confirmationUrl) { throw 'Retry allocated a different ticket' }
Check $retry $seatPattern 'Repeated submission returns original ticket'
Check (PostForm "$BaseUrl/user/bookTicket" @{journeyId=$match.Groups[1].Value;bookingRequestId='bad'} $passenger) 'booking form is invalid' 'Malformed booking request rejected'
function CheckPostDenied([string]$path, [hashtable]$body, $session, [string]$label) {
    $actual = 0
    try { $result = Invoke-WebRequest -UseBasicParsing "$BaseUrl$path" -Method Post -Body $body -WebSession $session; $actual = [int]$result.StatusCode }
    catch {
        if ($_.Exception.Response) { $actual = [int]$_.Exception.Response.StatusCode } else { throw }
    }
    if ($actual -ne 403) { throw "Failed: $label (expected 403, received $actual)" }
    Write-Host "PASS: $label"
}
CheckPostDenied '/login' @{username='demo';password='Demo123!'} $anonymous 'Login without CSRF rejected'
CheckPostDenied '/register' @{username='forged'} $anonymous 'Registration without CSRF rejected'
CheckPostDenied '/user/bookTicket' @{journeyId=$match.Groups[1].Value;seatNumber='CSRF';coachNumber='S1'} $passenger 'Booking without CSRF rejected'
CheckPostDenied '/admin/train' @{action='delete';trainId='1';csrfToken='wrong'} $admin 'Admin mutation with wrong CSRF rejected'
$foreignForm = Invoke-WebRequest -UseBasicParsing "$BaseUrl/login.jsp" -WebSession $other
$foreignToken = [regex]::Match($foreignForm.Content, 'name="csrfToken" value="([^"]+)"').Groups[1].Value
CheckPostDenied '/user/bookTicket' @{csrfToken=$foreignToken;journeyId=$match.Groups[1].Value;seatNumber='CSRF';coachNumber='S1'} $passenger 'CSRF token from another session rejected'
CheckDenied "$BaseUrl/logout" $passenger 405 'GET cannot log out'
Check (Invoke-WebRequest -UseBasicParsing "$BaseUrl/user/userDashboard.jsp" -WebSession $passenger) 'Demo Passenger' 'GET logout kept session active'
$ticketId = [regex]::Match($confirmationUrl, 'ticketId=(\d+)').Groups[1].Value
Check (PostForm "$BaseUrl/user/cancelTicket" @{ticketId=$ticketId} $passenger) 'Ticket cancelled' 'Cancellation from ticket interface'
$beforeLogout = Invoke-WebRequest -UseBasicParsing "$BaseUrl/login.jsp" -WebSession $passenger
$oldToken = [regex]::Match($beforeLogout.Content, 'name="csrfToken" value="([^"]+)"').Groups[1].Value
Check (PostForm "$BaseUrl/logout" @{} $passenger) 'Login' 'Logout with CSRF'
CheckPostDenied '/login' @{username='demo';password='Demo123!';csrfToken=$oldToken} $passenger 'Expired session token rejected'
Write-Host 'All local smoke checks passed.'
