$ErrorActionPreference = 'Stop'
$projectPath = Join-Path $PSScriptRoot 'spring-lab-01'
$pgBin = 'C:\Program Files\PostgreSQL\18\bin'
$clusterPath = Join-Path $PSScriptRoot '.lab10-postgres'
if (-not (Test-Path -LiteralPath (Join-Path $clusterPath 'PG_VERSION'))) {
    & (Join-Path $pgBin 'initdb.exe') -D $clusterPath -U lab10 -A trust --encoding=UTF8 --locale=C
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL initialization failed' }
}
& (Join-Path $pgBin 'pg_isready.exe') -h 127.0.0.1 -p 55432 -q
if ($LASTEXITCODE -ne 0) {
    Start-Process -FilePath (Join-Path $pgBin 'postgres.exe') -ArgumentList @('-D', ('"' + $clusterPath + '"'), '-p', '55432', '-h', '127.0.0.1') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $PSScriptRoot 'postgres.log') -RedirectStandardError (Join-Path $PSScriptRoot 'postgres-error.log')
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        Start-Sleep -Milliseconds 500
        & (Join-Path $pgBin 'pg_isready.exe') -h 127.0.0.1 -p 55432 -q
        if ($LASTEXITCODE -eq 0) { break }
    }
}
$exists = & (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p 55432 -U lab10 -d postgres -tAc "select 1 from pg_database where datname='springlab'"
if ($exists -ne '1') { & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 55432 -U lab10 springlab }
Set-Location -LiteralPath $projectPath
$jarPath = Join-Path $projectPath 'target\spring-lab-01-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) {
    & mvn '-Dmaven.test.skip=true' package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
}
& java -jar $jarPath '--spring.profiles.active=dev,lab10'
