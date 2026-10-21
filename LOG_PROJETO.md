# Log do Projeto - Back-end Orienta (P4 - Monólito Modular)

## Tecnologias e Arquitetura (P4)
* **Linguagem:** Java 17 (LTS)
* **Framework:** Spring Boot 3.4.5
* **Banco de Dados:** PostgreSQL (Local via Docker Compose / Supabase)
* **Migrations:** Flyway Community (`ddl-auto=validate`)
* **Serviço de E-mail:** Spring Mail com Mailpit Local (Custo Zero)
* **Documentação Viva:** SpringDoc OpenAPI 3.0 / Swagger UI (`/api/v1/swagger-ui.html`)
* **Padrão de Arquitetura:** Monólito Modular em Camadas (Model, Repository, Service, Controller, DTO)

---

## Histórico de Fases do Plano de Migração

### Fase 0 — Preservar e Preparar ✅ [CONCLUÍDA]
* **Objetivo:** Estabelecer linha de base segura sem perda do histórico.
* **Resolução:** Criação dos novos repositórios dedicados do 4º Período (`projeto-ads-p4-api` e `projeto-ads-p4-mobile`), recebendo a cópia fiel do legado P3 preservada.

### Fase 1 — Tornar a Execução Reproduzível ✅ [CONCLUÍDA]
* **Objetivo:** Garantir inicialização 100% local a custo zero e instruções claras.
* **Entregas Realizadas:**
  1. Configuração do `application.properties` desacoplada por variáveis de ambiente (`PORT`, `DB_URL`, `DB_USER`, `MAIL_HOST`).
  2. Criação do `.env.example` livre de credenciais sensíveis.
  3. Configuração do Flyway com migration `V1__baseline_schema.sql` cobrindo todas as 6 tabelas legadas.
  4. Configuração de `ddl-auto=validate` para que Flyway assuma o controle do schema.
  5. Criação do `docker-compose.yml` para PostgreSQL e Mailpit locais.
  6. Implementação do endpoint de Health Check `GET /api/health` retornando status UP em JSON.
  7. Validação do Maven Wrapper (`./mvnw`).
  8. **Status:** Pronto para o 1º Commit oficial de evolução do P4.

---

## Próximos Passos (Fase 2 & Fase 3)
* [ ] Fase 2: Criar GlobalExceptionHandler com `@ControllerAdvice`, Bean Validation e DTOs estritos.
* [ ] Fase 3: Integrar Spring Security com BCrypt e tokens para eliminar confiança cega em IDs.
* [ ] Fase 4: Padronização das rotas no prefixo `/api/v1` com Swagger interativo.