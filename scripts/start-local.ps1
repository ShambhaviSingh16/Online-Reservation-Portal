param([switch]$DebugServer, [switch]$SkipBuild, [int]$Port=8080)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
Set-Location $root
foreach($name in @('DB_URL','DB_USER','DB_PASSWORD')) {
    if([Environment]::GetEnvironmentVariable($name)) { throw "Clear $name before starting the isolated local database." }
}
& "$PSScriptRoot/start-database.ps1"
if(-not $SkipBuild) {
    & mvn.cmd -B package
    if($LASTEXITCODE -ne 0) { throw 'Build failed.' }
}
if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use." }
$war=Join-Path $root 'target/TrackEase.war'
if(-not (Test-Path $war)) { throw 'Build the WAR before using -SkipBuild.' }
$arguments=@('-jar',('"'+$war+'"'),"--server.port=$Port",'--server.address=127.0.0.1','--debug=false')
if($DebugServer) { $arguments=@('-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8000')+$arguments }
$java=(Get-Command java.exe).Source
if($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/java.exe")) { $java=Join-Path $env:JAVA_HOME 'bin/java.exe' }
$process=Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root/.local/app.log" -RedirectStandardError "$root/.local/app-error.log"
@{pid=$process.Id;started=$process.StartTime.ToUniversalTime().Ticks;war=$war} | ConvertTo-Json | Set-Content "$root/.local/app-process.json"
Write-Host "Starting Spring Boot at http://localhost:$Port. Logs: .local/app.log"
$process.WaitForExit()
if($process.ExitCode -ne 0) { Get-Content "$root/.local/app-error.log" -Tail 20; throw 'Application exited. See .local/app.log.' }
