#!/usr/bin/env bash
# Roda UMA VEZ, manualmente, na instância recém-criada (Lightsail Ubuntu), antes do
# primeiro deploy — instala Docker, cria swap de 2GB (a instância de 1GB de RAM
# sofre com picos de memória no build/migrations sem isso) e clona o repositório.
#
# Uso (na instância, via SSH): curl -fsSL <raw-url-deste-arquivo> | bash
# ou copie o repo primeiro e rode ./infra/scripts/setup-host.sh

set -euo pipefail

echo "== Docker =="
if ! command -v docker &> /dev/null; then
  curl -fsSL https://get.docker.com | sh
  sudo usermod -aG docker "$USER"
fi

echo "== Swap de 2GB =="
if [ ! -f /swapfile ]; then
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "== Repositório =="
if [ ! -d /opt/poupito ]; then
  sudo mkdir -p /opt/poupito
  sudo chown "$USER":"$USER" /opt/poupito
  git clone https://github.com/JokerFoxyy/Poupito.git /opt/poupito
fi

echo
echo "Próximos passos manuais:"
echo "1. cp /opt/poupito/infra/.env.prod.example /opt/poupito/infra/.env e preencher com valores reais"
echo "2. cd /opt/poupito && docker compose -f infra/docker-compose.prod.yml --env-file infra/.env up -d"
echo "3. ./infra/scripts/configure-s3-lifecycle.sh <bucket>  (uma vez, após criar o bucket)"
echo "4. Agendar infra/scripts/backup.sh no cron (ex.: crontab -e -> 0 3 * * * /opt/poupito/infra/scripts/backup.sh)"
