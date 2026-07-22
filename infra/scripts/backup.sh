#!/usr/bin/env bash
# Backup do Postgres de produção -> S3, com retenção local de 7 dias
# (o lifecycle de 30 dias no bucket cuida da retenção remota).
#
# Uso: ./backup.sh (agendado via cron/systemd timer no host, não roda em container)
# Requer no ambiente: POSTGRES_USER, POSTGRES_DB, BACKUP_S3_BUCKET, AWS_REGION
# (mesmos valores do .env de produção — este script faz `source` nele).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-$SCRIPT_DIR/../.env}"
BACKUP_DIR="${BACKUP_DIR:-/opt/guaranin/backups}"
CONTAINER="${POSTGRES_CONTAINER:-guaranin-postgres-prod}"

# shellcheck source=/dev/null
[ -f "$ENV_FILE" ] && source "$ENV_FILE"

: "${POSTGRES_USER:?defina POSTGRES_USER (no .env ou no ambiente)}"
: "${POSTGRES_DB:?defina POSTGRES_DB}"
: "${BACKUP_S3_BUCKET:?defina BACKUP_S3_BUCKET}"

mkdir -p "$BACKUP_DIR"
timestamp="$(date +%Y%m%d-%H%M%S)"
file="$BACKUP_DIR/guaranin-$timestamp.sql.gz"

docker exec "$CONTAINER" pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$file"

aws s3 cp "$file" "s3://$BACKUP_S3_BUCKET/postgres/$(basename "$file")" \
  --region "${AWS_REGION:-sa-east-1}" --only-show-errors

# retenção local curta — o histórico de verdade fica no S3 (lifecycle de 30 dias)
find "$BACKUP_DIR" -name '*.sql.gz' -mtime +7 -delete

echo "Backup enviado: s3://$BACKUP_S3_BUCKET/postgres/$(basename "$file")"
