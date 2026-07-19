# Guaranin — Conformidade LGPD & Confidencialidade

> Lei 13.709/2018 (LGPD). Documento vivo — atualizar quando novos dados pessoais forem coletados.
> Criado na Sessão S (2026-07-10). **Nota:** guia técnico de engenharia, não parecer jurídico.

## 1. Papéis

- **Controlador:** o titular do projeto (uso pessoal hoje; vira controlador formal na fase SaaS, quando precisará de encarregado/DPO e política de privacidade pública).
- **Operador:** provedor de hospedagem (AWS Lightsail/EC2 — sessão #21).

## 2. Inventário de dados pessoais

| Dado | Categoria LGPD | Base legal (Art. 7º) | Finalidade | Onde |
|---|---|---|---|---|
| E-mail | Dado pessoal | Execução de contrato (uso do app) | Identificação/login | `users.email` |
| Senha (hash) | Dado pessoal (credencial) | Execução de contrato | Autenticação | `users.password_hash` (BCrypt, irreversível) |
| Transações, contas, categorias, metas, investimentos | Pessoais (revelam hábitos financeiros) | Execução de contrato / legítimo interesse do próprio titular | Função central do app | tabelas do domínio, escopadas por `user_id` |
| IP (rate limiting) | Pessoal | Legítimo interesse (segurança) | Anti-brute-force | memória volátil, **não persistido** |

Não coletamos: CPF, telefone, endereço, dado de cartão real (só metadados: nome do cartão, dia de fechamento/vencimento), nem dados sensíveis (Art. 5º II).

## 3. Direitos do titular (Art. 18) e onde são atendidos

| Direito | Como é atendido |
|---|---|
| Confirmação e acesso | `GET /v1/auth/me` + `GET /v1/account/export` (dump completo em JSON) |
| Portabilidade | `GET /v1/account/export` (JSON estruturado e legível por máquina) |
| Correção | Edição de conta/categoria/transação já disponível; troca de senha (backlog) |
| Eliminação | `DELETE /v1/account` — apaga usuário e **todos** os dados vinculados numa transação |
| Revogação de sessão | `POST /v1/auth/logout` revoga o refresh token no servidor |

## 4. Princípios aplicados (Art. 6º)

- **Minimização:** coletamos só e-mail + dados que o próprio usuário lança. Sem rastreadores, sem analytics de terceiros, sem compartilhamento.
- **Segurança (Art. 46):** senha em BCrypt; sessão em cookie httpOnly; TLS em trânsito (#21); acesso escopado por usuário; segredos fora do código. Ver `threat-model-stride.md`.
- **Necessidade/finalidade:** cada dado tem finalidade declarada acima; nada é usado para outro fim.
- **Retenção:** dados existem enquanto a conta existir; exclusão da conta remove tudo. Refresh tokens expiram em 30 dias e são limpos.

## 5. Confidencialidade dos dados

- **Em trânsito:** TLS 1.2+ (Caddy/Let's Encrypt) em produção — pendente da sessão #21; em dev, http local.
- **Em repouso:** senha com hash irreversível; refresh token com hash SHA-256 (o banco nunca guarda o token em claro). Demais dados financeiros **não são criptografados em campo** hoje — a proteção é o controle de acesso ao banco (rede interna, credencial por env) + backup restrito. Criptografia de disco/campo é backlog para a fase SaaS.
- **Backups:** `pg_dump` → S3 com bucket restrito e lifecycle de 30 dias (sessão #21).

## 6. Resposta a incidente (Art. 48)

Na fase SaaS: em caso de incidente com risco a titulares, notificar ANPD e titulares em prazo razoável. Hoje (uso pessoal, 1 titular) o processo é informal, mas o ponto de partida é o threat model e os logs da plataforma.

## 7. Pendências para a fase SaaS

Política de privacidade pública · tela de consentimento no cadastro · encarregado (DPO) · registro de operações (ROPA) · DPA com o provedor de hospedagem · troca/reset de senha · audit log de acesso a dados.
