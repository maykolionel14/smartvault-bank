$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$example = Join-Path $root '.env.example'
$target = Join-Path $root '.env'
if (Test-Path $target) { Write-Host 'Ya existe .env; no se sobrescribio.'; exit 0 }
if (!(Test-Path $example)) { throw 'No se encuentra .env.example' }
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
function Get-RandomBytes([int]$n) {
    $b = New-Object byte[] $n
    $rng.GetBytes($b)
    return ,$b
}
function Get-HexSecret() { return [BitConverter]::ToString((Get-RandomBytes 32)).Replace('-','') }
$lines = Get-Content $example
$lines = $lines | ForEach-Object {
    if ($_ -match '^DB_PASSWORD=') { 'DB_PASSWORD='+(Get-HexSecret) }
    elseif ($_ -match '^JWT_SECRET=') { 'JWT_SECRET='+(Get-HexSecret) }
    elseif ($_ -match '^SERVICE_KEY=') { 'SERVICE_KEY='+(Get-HexSecret) }
    else { $_ }
}
[IO.File]::WriteAllLines($target,[string[]]$lines,[Text.UTF8Encoding]::new($false))
$rng.Dispose()
Write-Host 'Archivo .env creado con secretos aleatorios. Mantenlo privado.'
