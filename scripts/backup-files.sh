#!/usr/bin/env bash
# ====================================================================
# Healthy Choco Bytes - Media / Uploads Backup Script
# Archives /var/lib/hcb/uploads and static assets
# ====================================================================

set -euo pipefail

UPLOADS_DIR="/var/lib/hcb/uploads"
BACKUP_DIR="/var/backups/hcb/files"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/hcb_uploads_${TIMESTAMP}.tar.gz"
RETENTION_DAYS=30

mkdir -p "${BACKUP_DIR}"

if [ -d "${UPLOADS_DIR}" ]; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Archiving uploads from ${UPLOADS_DIR}..."
    tar -czf "${BACKUP_FILE}" -C "${UPLOADS_DIR}" .
    chmod 600 "${BACKUP_FILE}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Uploads backup created: ${BACKUP_FILE}"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Directory ${UPLOADS_DIR} does not exist yet. Skipping."
fi

# Rotate backups
find "${BACKUP_DIR}" -type f -name "hcb_uploads_*.tar.gz" -mtime +"${RETENTION_DAYS}" -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] File backup workflow completed successfully."
