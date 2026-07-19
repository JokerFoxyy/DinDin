# Guaranin — Modelo de Ameaças (STRIDE)

> Criado na Sessão S (2026-07-10). Reavaliar a cada mudança que toque autenticação, dados
> pessoais, fronteiras de rede ou dependências. Status: ✅ mitigado · 🟡 parcial · 🔴 aberto.

## 1. Ativos protegidos

| Ativo | Sensibilidade | Onde vive |
|---|---|---|
| Credenciais (senha) | Crítica | `users.password_hash` (BCrypt) |
| Tokens de sessão | Crítica | Cookie httpOnly (access) + `refresh_tokens` (hash) |
| Dados financeiros (transações, saldos, faturas) | Alta (pessoais + revelam hábitos) | Postgres |
| Dados pessoais (email) | Média (PII sob LGPD) | `users.email` |
| Segredos de infraestrutura | Crítica | Variáveis de ambiente |

## 2. Diagrama de fluxo (textual) e fronteiras de confiança

```
[Navegador do usuário]  --TF1-->  [Caddy/TLS + SPA Angular]  --TF2-->  [API Spring Boot]  --TF3-->  [PostgreSQL]
                                                                              |
                                                                        (@Scheduled jobs)
```

- **TF1 (internet → borda):** não confiável. TLS obrigatório em prod (Caddy/Let's Encrypt).
- **TF2 (SPA → API):** same-origin (`/api` via proxy/Caddy). Autenticação por cookie httpOnly.
- **TF3 (API → banco):** rede interna do Docker/host; credenciais por env.

## 3. Análise STRIDE

### S — Spoofing (falsificação de identidade)
| Ameaça | Mitigação | Status |
|---|---|---|
| Roubo de sessão via token no `localStorage` (XSS) | Access token em cookie **httpOnly** — inacessível ao JS | ✅ (Parte C) |
| Força bruta de senha no login | **Rate limiting** por IP+email → 429; BCrypt lento | ✅ (B2/B3) |
| Reuso de refresh token roubado | Refresh token opaco, **rotacionado** a cada uso e **revogável** no banco; hash SHA-256 (roubo do banco não revela o token) | ✅ (C1) |
| Ausência de MFA | Aceito para v1 (uso pessoal); backlog para fase SaaS | 🟡 |

### T — Tampering (adulteração)
| Ameaça | Mitigação | Status |
|---|---|---|
| Adulteração do JWT | Assinatura HS256 com segredo ≥ 32 bytes; token inválido → 401 | ✅ |
| SQL injection | JPA parametrizado / Specifications; sem concatenação de input | ✅ |
| Schema driftar do código | Flyway + `ddl-auto: validate` | ✅ |
| Payload malicioso / massa de dados | Bean Validation em todo DTO; `amount > 0`, tamanhos, formatos | ✅ |

### R — Repudiation (repúdio)
| Ameaça | Mitigação | Status |
|---|---|---|
| Usuário/atacante nega ação financeira | `created_at` em toda entidade; **audit log de eventos sensíveis** (login, refresh, logout, exclusão de conta) | 🟡 backlog (fase SaaS) |
| Logs adulterados | Fora de escopo v1; em prod, logs para stdout coletados pela plataforma | 🔴 |

### I — Information Disclosure (vazamento)
| Ameaça | Mitigação | Status |
|---|---|---|
| Senha vazar | BCrypt (nunca texto claro); nenhum DTO expõe hash | ✅ |
| Segredo de dev em produção | **Fail-fast** em profile prod se `JWT_SECRET`/`DB_PASSWORD` forem default | ✅ (B1) |
| Token vazar em log | Revisão: senha/token/refresh nunca logados; erro genérico ao cliente | ✅ (B5) |
| Enumeração de usuário no login | Erro genérico "Email ou senha inválidos" | ✅ |
| Enumeração de usuário no registro | 409 mantido por usabilidade, **mitigado por rate limiting** — risco aceito | 🟡 |
| Acesso a dados de outro usuário | Toda query escopada por `user_id`; recurso alheio → **404** | ✅ |
| Swagger/api-docs público em prod | Fechado em profile prod | ✅ (B4) |
| Tráfego em claro | TLS via Caddy (deploy #21); HSTS em prod | 🟡 (depende #21) |
| Dados sensíveis em URL | Filtros vão em query params **não sensíveis** (mês, ids); nunca senha/token | ✅ |

### D — Denial of Service
| Ameaça | Mitigação | Status |
|---|---|---|
| Flood de login/registro | Rate limiting → 429 | ✅ (B2) |
| Payload gigante | Limite de tamanho de request do Spring (default) + limites de campo | 🟡 |
| Exaustão de conexão do banco | Pool Hikari (default) + máquina única; tuning fica p/ #21 | 🟡 |

### E — Elevation of Privilege
| Ameaça | Mitigação | Status |
|---|---|---|
| Acesso a endpoint sem auth | Spring Security **deny-by-default**; só register/login/refresh/health/swagger públicos | ✅ |
| Escalar para outro usuário | Identidade vem do token assinado; recursos escopados por `user_id` | ✅ |
| Forjar ajuste de fatura | `INVOICE_ADJUSTMENT` rejeitado na API (reservado ao sistema) | ✅ |

## 4. Riscos aceitos (v1, uso pessoal)

1. Sem MFA — reavaliar ao abrir cadastro público.
2. Enumeração de e-mail no registro — mitigada por rate limiting.
3. Audit log completo e observabilidade — backlog (sessão #29 / fase SaaS).
4. Rate limiter in-memory (não distribuído) — válido enquanto for single-instance.

## 5. Backlog de segurança (fase SaaS)

MFA/TOTP · audit log imutável · rotação de segredos (Secrets Manager) · WAF/Cloudflare (#17) · pen-test · criptografia de campo para PII sensível · alertas de acesso anômalo.
