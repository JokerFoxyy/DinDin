#!/usr/bin/env bash
# Roda UMA VEZ, manualmente, depois de criar o bucket de backup — aplica a regra
# de lifecycle que expira objetos com mais de 30 dias (evita custo de S3 crescendo
# indefinidamente com backups diários).
#
# Uso: ./configure-s3-lifecycle.sh <nome-do-bucket>

set -euo pipefail

bucket="${1:?uso: ./configure-s3-lifecycle.sh <nome-do-bucket>}"

aws s3api put-bucket-lifecycle-configuration \
  --bucket "$bucket" \
  --lifecycle-configuration '{
    "Rules": [
      {
        "ID": "expire-backups-30d",
        "Filter": { "Prefix": "postgres/" },
        "Status": "Enabled",
        "Expiration": { "Days": 30 }
      }
    ]
  }'

echo "Lifecycle de 30 dias aplicado ao bucket $bucket (prefixo postgres/)."
