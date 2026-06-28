# Enregistre (ou met à jour) la tâche planifiée de sauvegarde quotidienne PostgreSQL (DGCPT).
# Pré-requis : un fichier d'identifiants pg_dump dans %APPDATA%\postgresql\pgpass.conf
#   (ligne : hote:port:base:utilisateur:motdepasse), pour que la tâche s'exécute sans invite.
# Usage (PowerShell) :  .\scripts\installer-sauvegarde-planifiee.ps1
param(
    [string]$ScriptPath = (Join-Path $PSScriptRoot 'sauvegarde-postgres.ps1'),
    [string]$TaskName   = 'DGCPT-Sauvegarde-PostgreSQL',
    [string]$Heure      = '01:00'
)
$ErrorActionPreference = 'Stop'

$action  = New-ScheduledTaskAction -Execute 'powershell.exe' `
            -Argument ("-NonInteractive -NoProfile -ExecutionPolicy Bypass -File `"{0}`"" -f $ScriptPath)
$trigger = New-ScheduledTaskTrigger -Daily -At $Heure
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Hours 1)

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings `
    -Description 'Sauvegarde quotidienne PostgreSQL DGCPT (Missions & Parc)' -Force | Out-Null

Write-Output "Tâche planifiée « $TaskName » enregistrée (quotidienne à $Heure)."
Write-Output "Vérifier : Get-ScheduledTask -TaskName '$TaskName' ; lancer un test : Start-ScheduledTask -TaskName '$TaskName'."
