#!/usr/bin/env bash
# =============================================================================
# 03-restore.sh - Restauracion de la BD shareYourStory desde un backup
# =============================================================================
# Restaura un volcado generado por 03-backup.sh. El fichero incluye
# 'DROP TABLE IF EXISTS' y 'CREATE DATABASE IF NOT EXISTS', por lo que deja la
# base de datos exactamente en el estado del backup.
#
# Uso:
#   bash db/03-restore.sh db/backups/shareYourStory_AAAAMMDD_HHMMSS.sql
#
# Variables de entorno opcionales:
#   DB_HOST=127.0.0.1  DB_PORT=3306
#   RESTORE_USER=app_admin  RESTORE_PASSWORD=app_admin_pwd
# =============================================================================
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "Uso: $0 <fichero_backup.sql>" >&2
  exit 1
fi

BACKUP_FILE="$1"
if [ ! -f "$BACKUP_FILE" ]; then
  echo "ERROR: no existe el fichero '$BACKUP_FILE'" >&2
  exit 1
fi

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${RESTORE_USER:-app_admin}"
DB_PASS="${RESTORE_PASSWORD:-app_admin_pwd}"

echo "Restaurando desde '${BACKUP_FILE}'..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" < "$BACKUP_FILE"
echo "OK - restauracion completada."
