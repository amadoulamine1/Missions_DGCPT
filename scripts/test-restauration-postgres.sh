#!/usr/bin/env bash
# Test de restauration : restaure la DERNIÈRE sauvegarde dans une base JETABLE, contrôle sa cohérence,
# puis la supprime. Valide périodiquement que les sauvegardes sont réellement restaurables,
# SANS toucher à la base de production. À planifier (hebdomadaire) ou à lancer à la demande.
set -euo pipefail

DB_NAME="${DB_NAME:-missions_parc}"
DB_USER="${DB_USER:-missions_parc}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DEST="${BACKUP_DIR:-/var/backups/dgcpt}"
TEST_DB="${TEST_DB:-${DB_NAME}_test_restore}"

export PGPASSWORD="${DB_PASSWORD:-}"
psql_admin() { psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres "$@"; }

# Dernière sauvegarde disponible
FICHIER="$(ls -1t "$DEST"/${DB_NAME}-*.dump 2>/dev/null | head -1 || true)"
[ -n "$FICHIER" ] || { echo "Aucune sauvegarde trouvée dans $DEST"; exit 1; }
echo "Test de restauration de « $FICHIER » dans la base jetable « $TEST_DB »."

# Base jetable propre (supprimée en sortie, quoi qu'il arrive)
trap 'psql_admin -c "DROP DATABASE IF EXISTS \"$TEST_DB\";" >/dev/null 2>&1 || true' EXIT
psql_admin -c "DROP DATABASE IF EXISTS \"$TEST_DB\";"
psql_admin -c "CREATE DATABASE \"$TEST_DB\";"

pg_restore -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TEST_DB" --no-owner "$FICHIER"

# Contrôles de cohérence : présence de tables et d'un historique Flyway appliqué
NB_TABLES="$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TEST_DB" -tAc \
  "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';")"
NB_MIGR="$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$TEST_DB" -tAc \
  "SELECT count(*) FROM flyway_schema_history WHERE success;" 2>/dev/null || echo 0)"
echo "Tables restaurées : ${NB_TABLES} — migrations Flyway appliquées : ${NB_MIGR}"

if [ "${NB_TABLES:-0}" -gt 0 ] && [ "${NB_MIGR:-0}" -gt 0 ]; then
  echo "OK : la sauvegarde est restaurable (base jetable supprimée)."
else
  echo "ÉCHEC : restauration incomplète."; exit 2
fi
