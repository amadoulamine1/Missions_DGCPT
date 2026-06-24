#!/usr/bin/env bash
# Sauvegarde quotidienne de la base PostgreSQL (DGCPT — Missions & Parc).
# À planifier via cron, ex. tous les jours à 1h :
#   0 1 * * *  /opt/dgcpt/scripts/sauvegarde-postgres.sh >> /var/log/dgcpt-sauvegarde.log 2>&1
set -euo pipefail

DB_NAME="${DB_NAME:-missions_parc}"
DB_USER="${DB_USER:-missions_parc}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DEST="${BACKUP_DIR:-/var/backups/dgcpt}"
RETENTION_JOURS="${RETENTION_JOURS:-30}"

mkdir -p "$DEST"
HORODATAGE="$(date +%Y%m%d-%H%M%S)"
FICHIER="$DEST/${DB_NAME}-${HORODATAGE}.dump"

# Dump compressé (format custom, restaurable avec pg_restore)
PGPASSWORD="${DB_PASSWORD:-}" pg_dump -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -F c -f "$FICHIER" "$DB_NAME"
echo "$(date '+%F %T') Sauvegarde créée : $FICHIER"

# Rotation : supprime les sauvegardes de plus de RETENTION_JOURS jours
find "$DEST" -name "${DB_NAME}-*.dump" -type f -mtime +"$RETENTION_JOURS" -delete
echo "$(date '+%F %T') Rotation effectuée (> ${RETENTION_JOURS} jours supprimés)."
