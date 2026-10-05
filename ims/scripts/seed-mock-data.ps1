# Loads scripts/seed-mock-data.sql into the local IMS Postgres container.
# Requires docker-compose postgres (container name: my-postgres).

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "seed-mock-data.sql"
$container = if ($env:IMS_PG_CONTAINER) { $env:IMS_PG_CONTAINER } else { "my-postgres" }
$dbUser = if ($env:IMS_PG_USER) { $env:IMS_PG_USER } else { "anikettcodes" }
$dbName = if ($env:IMS_PG_DB) { $env:IMS_PG_DB } else { "ims" }

if (-not (Test-Path $sqlFile)) {
    Write-Error "SQL file not found: $sqlFile"
}

$running = docker inspect -f "{{.State.Running}}" $container 2>$null
if ($running -ne "true") {
    Write-Error "Container '$container' is not running. Start it with: docker compose up -d (from ims/)"
}

Write-Host "Seeding mock data into $dbName on $container ..."
Get-Content -Raw -Path $sqlFile | docker exec -i $container psql -v ON_ERROR_STOP=1 -U $dbUser -d $dbName
if ($LASTEXITCODE -ne 0) {
    Write-Error "psql failed with exit code $LASTEXITCODE"
}
Write-Host "Done. GET http://localhost:8080/api/v1/categories should now return seeded categories."
