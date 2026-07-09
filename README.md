# FinanceIA

App de gestão financeira pessoal — transações, contas/cartões com fatura, fixos recorrentes, orçamentos, dashboard, investimentos e metas.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `api/` | API REST — Java 21 + Spring Boot 3.5 + PostgreSQL + Flyway |
| `web/` | Frontend Angular 20 (a partir da sessão #3) |
| `infra/` | Docker Compose e scripts |
| `docs/` | Plano SDD e documentos por sessão |

## Rodando em desenvolvimento

```bash
# 1. Banco
docker compose -f infra/docker-compose.yml up -d

# 2. API (requer JDK 21)
cd api
./mvnw spring-boot:run
```

- Health: http://localhost:8080/api/actuator/health
- Swagger: http://localhost:8080/api/swagger-ui.html

Qualidade: cobertura mínima de 90% de linha (JaCoCo, quebra o build no `verify`).

Roadmap e arquitetura: [docs/PLANO-SDD.md](docs/PLANO-SDD.md)
