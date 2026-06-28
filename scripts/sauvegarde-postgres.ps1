# Sauvegarde quotidienne de la base PostgreSQL (DGCPT - Missions & Parc) - version Windows.
# À planifier via le Planificateur de tâches (voir scripts/installer-sauvegarde-planifiee.ps1).
# Le mot de passe est lu par pg_dump dans %APPDATA%\postgresql\pgpass.conf (aucun secret ici).
param(
    [string]$DbName     = $(if ($env:DB_NAME)        { $env:DB_NAME }        else { 'missions_parc' }),
    [string]$DbUser     = $(if ($env:DB_USER)        { $env:DB_USER }        else { 'postgres' }),
    [string]$DbHost     = $(if ($env:DB_HOST)        { $env:DB_HOST }        else { 'localhost' }),
    [int]   $DbPort     = $(if ($env:DB_PORT)        { [int]$env:DB_PORT }   else { 5432 }),
    [string]$BackupDir  = $(if ($env:BACKUP_DIR)     { $env:BACKUP_DIR }     else { Join-Path $env:ProgramData 'DGCPT\backups' }),
    [int]   $RetentionJours = $(if ($env:RETENTION_JOURS) { [int]$env:RETENTION_JOURS } else { 30 }),
    [string]$PgBin      = $(if ($env:PG_BIN)         { $env:PG_BIN }         else { 'C:\Program Files\PostgreSQL\18\bin' })
)
$ErrorActionPreference = 'Stop'

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$horodatage = Get-Date -Format 'yyyyMMdd-HHmmss'
$fichier = Join-Path $BackupDir "$DbName-$horodatage.dump"
$pgDump  = Join-Path $PgBin 'pg_dump.exe'

# Dump compressé (format custom, restaurable avec pg_restore)
& $pgDump -h $DbHost -p $DbPort -U $DbUser -F c -f $fichier $DbName
if ($LASTEXITCODE -ne 0) { throw "pg_dump a échoué (code $LASTEXITCODE)" }
Write-Output ("{0} Sauvegarde créée : {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $fichier)

# Rotation : supprime les sauvegardes de plus de RetentionJours jours
$limite = (Get-Date).AddDays(-$RetentionJours)
Get-ChildItem -Path $BackupDir -Filter "$DbName-*.dump" -File |
    Where-Object { $_.LastWriteTime -lt $limite } |
    Remove-Item -Force
Write-Output ("{0} Rotation effectuée (> {1} jours supprimés)." -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $RetentionJours)
