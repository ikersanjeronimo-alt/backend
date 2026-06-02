#!/usr/bin/env bash
# =============================================================================
# 03-backup.sh - Copia de seguridad de la BD shareYourStory (MySQL)
# =============================================================================
# Genera un volcado completo (esquema + datos + procedimientos + triggers +
# eventos) con mysqldump, usando el usuario 'app_admin' (ver db/02).
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
  --routines --triggers --events \
  --add-drop-table \
  --databases "$DB_NAME" > "$OUT_FILE"

echo "OK -> ${OUT_FILE}"
echo "Tamano: $(du -h "$OUT_FILE" | cut -f1)"
