#!/usr/bin/env bash
# =============================================================================
# 03-backup.sh - Copia de seguridad de la BD shareYourStory (MySQL)
# =============================================================================
# Genera un volcado completo (esquema + datos + procedimientos + triggers +
# eventos) con mysqldump, usando el usuario 'app_admin' (ver db/02).
#
# Se incluye --no-tablespaces porque 'app_admin' tiene privilegios solo a nivel
# de esquema (sin PROCESS global); sin esa opcion, mysqldump 8.x aborta con
# "Access denied; you need (at least one of) the PROCESS privilege(s)".
#
# Uso:
#   bash db/03-backup.sh
#
# Variables de entorno opcionales (con sus valores por defecto):
#   DB_NAME=shareYourStory  DB_HOST=127.0.0.1  DB_PORT=3306
#   BACKUP_USER=app_admin   BACKUP_PASSWORD=app_admin_pwd
# =============================================================================
set -euo pipefail

DB_NAME="${DB_NAME:-shareYourStory}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${BACKUP_USER:-app_admin}"
DB_PASS="${BACKUP_PASSWORD:-app_admin_pwd}"

OUT_DIR="$(cd "$(dirname "$0")" && pwd)/backups"
mkdir -p "$OUT_DIR"
STAMP="$(date +%Y%m%d_%H%M%S)"
OUT_FILE="${OUT_DIR}/${DB_NAME}_${STAMP}.sql"

echo "Creando copia de seguridad de '${DB_NAME}'..."
mysqldump \
  -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
  --single-transaction \
  --no-tablespaces \
  --routines --triggers --events \
  --add-drop-table \
  --databases "$DB_NAME" > "$OUT_FILE"

echo "OK -> ${OUT_FILE}"
echo "Tamano: $(du -h "$OUT_FILE" | cut -f1)"
