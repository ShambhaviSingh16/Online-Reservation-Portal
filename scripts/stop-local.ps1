param([switch]$KeepDatabase)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$statePath=Join-Path $root '.local/app-process.json'
if(Test-Path $statePath) {
    $state=Get-Content $statePath -Raw | ConvertFrom-Json
    $process=Get-Process -Id $state.pid -ErrorAction SilentlyContinue
    if($process -and $process.StartTime.ToUniversalTime().Ticks -eq $state.started) {
        Stop-Process -Id $process.Id
        Write-Host 'TrackEase stopped.'
    }
    Remove-Item -LiteralPath $statePath
}
if(-not $KeepDatabase) {
    $pg=Join-Path $root '.local/pgsql/bin/pg_ctl.exe'
    if(Test-Path $pg) { & $pg -D "$root/.local/pgdata" -m fast -w stop }
}
