#!/usr/bin/env bash
# ====================================================================
# Healthy Choco Bytes - Database Backup Script
# Creates compressed mysqldump/mariadb-dump with timestamp & rotates
# ====================================================================

set -euo pipefail

BACKUP_DIR="/var/backups/hcb/database"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/hcb_db_${TIMESTAMP}.sql.gz"
RETENTION_DAYS=30

DB_NAME="hcb"
DB_USER="hcb_user"

# Ensure backup directory exists
mkdir -p "${BACKUP_DIR}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting MariaDB backup of '${DB_NAME}'..."

# Execute dump using ~/.my.cnf credentials for hcb_user or defaults
if command -v mariadb-dump &> /dev/null; then
    DUMP_CMD="mariadb-dump"
else
    DUMP_CMD="mysqldump"
fi

${DUMP_CMD} \
    --user="${DB_USER}" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    "${DB_NAME}" | gzip -9 > "${BACKUP_FILE}"

# Restrict permissions
chmod 600 "${BACKUP_FILE}"

BACKUP_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup created: ${BACKUP_FILE} (${BACKUP_SIZE})"

# Delete backups older than retention window
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Purging backups older than ${RETENTION_DAYS} days..."
find "${BACKUP_DIR}" -type f -name "hcb_db_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Database backup workflow completed successfully."
