#!/usr/bin/env bash
# Restauration d'une sauvegarde PostgreSQL (DGCPT). ATTENTION : remplace les données de la base cible.
# Usage : ./restauration-postgres.sh /var/backups/dgcpt/missions_parc-AAAAMMJJ-HHMMSS.dump
set -euo pipefail

FICHIER="${1:?Usage: $0 <fichier.dump>}"
DB_NAME="${DB_NAME:-missions_parc}"
DB_USER="${DB_USER:-missions_parc}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

echo "Restauration de « $FICHIER » dans « $DB_NAME » — les données actuelles seront remplacées."
read -r -p "Confirmer ? [oui/non] " REP
[ "$REP" = "oui" ] || { echo "Annulé."; exit 1; }

PGPASSWORD="${DB_PASSWORD:-}" pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" --clean --if-exists "$FICHIER"
echo "Restauration terminée."
