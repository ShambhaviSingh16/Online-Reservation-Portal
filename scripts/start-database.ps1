$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$pg = Join-Path $root '.local/pgsql/bin'
$data = Join-Path $root '.local/pgdata'
if (-not (Test-Path "$pg/pg_ctl.exe")) {
    throw 'PostgreSQL binaries missing. Extract the EDB Windows binaries ZIP into .local (creating .local/pgsql). See README.md.'
}
$config = Join-Path $root 'local.properties'
if (-not (Test-Path $config)) {
    $password = [Guid]::NewGuid().ToString('N')
    "db.url=jdbc:postgresql://127.0.0.1:5433/trackease`ndb.user=trackease`ndb.password=$password" | Set-Content $config -Encoding ASCII
}
$properties = ConvertFrom-StringData (Get-Content $config -Raw)
if ($properties['db.url'] -ne 'jdbc:postgresql://127.0.0.1:5433/trackease' -or $properties['db.user'] -ne 'trackease') {
    throw 'This script manages only the workspace database at 127.0.0.1:5433/trackease with user trackease.'
}
if (-not (Test-Path "$data/PG_VERSION")) {
    $passwordFile = Join-Path $root '.local/init-password.txt'
    try {
        $properties['db.password'] | Set-Content $passwordFile -Encoding ASCII
        & "$pg/initdb.exe" -D $data -U trackease --pwfile=$passwordFile -A scram-sha-256 -E UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL initialization failed.' }
    } finally { Remove-Item -LiteralPath $passwordFile -ErrorAction SilentlyContinue }
}
& "$pg/pg_isready.exe" -h 127.0.0.1 -p 5433 -U trackease -d trackease *> $null
if ($LASTEXITCODE -ne 0) {
    & "$pg/pg_ctl.exe" -D $data -l "$root/.local/postgresql.log" -o '-p 5433 -h 127.0.0.1' -w start
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed. See .local/postgresql.log.' }
}
$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $properties['db.password']
    $exists = & "$pg/psql.exe" -h 127.0.0.1 -p 5433 -U trackease -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='trackease'"
    if ($LASTEXITCODE -ne 0) { throw 'Database connection failed.' }
    if ($exists -ne '1') {
        & "$pg/createdb.exe" -h 127.0.0.1 -p 5433 -U trackease trackease
        if ($LASTEXITCODE -ne 0) { throw 'Database creation failed.' }
    }
    & "$pg/psql.exe" -h 127.0.0.1 -p 5433 -U trackease -d trackease -v ON_ERROR_STOP=1 -f "$root/database/schema.sql" -f "$root/database/demo.sql"
    if ($LASTEXITCODE -ne 0) { throw 'Database setup failed.' }
} finally { $env:PGPASSWORD = $previousPassword }
