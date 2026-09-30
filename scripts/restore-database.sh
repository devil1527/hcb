#!/usr/bin/env bash
# ====================================================================
# Healthy Choco Bytes - Database Restore Script
# Safely restores a gzipped SQL dump into the MariaDB database
# Usage: ./restore-database.sh /path/to/backup.sql.gz
# ====================================================================

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <path_to_backup_file.sql.gz>"
    exit 1
fi

BACKUP_FILE="$1"
DB_NAME="hcb"
DB_USER="hcb_user"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "Error: Backup file '${BACKUP_FILE}' does not exist."
    exit 1
fi

read -rp "WARNING: This will overwrite data in database '${DB_NAME}'. Are you sure? (yes/no): " CONFIRM
if [ "${CONFIRM}" != "yes" ]; then
    echo "Restore aborted by user."
    exit 0
fi

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Restoring database from '${BACKUP_FILE}'..."

gunzip -c "${BACKUP_FILE}" | mariadb --user="${DB_USER}" -p "${DB_NAME}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Database '${DB_NAME}' restored successfully."
