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

### Fase 2 — Criar Base de Qualidade e Segurança ✅ [CONCLUÍDA]
* **Objetivo:** Blindar contratos, padronizar erros e sanitizar dados sensíveis.
* **Entregas Realizadas:**
  1. Criação do `GlobalExceptionHandler` interceptando `MethodArgumentNotValidException`, `RegraNegocioException` e `RecursoNaoEncontradoException`.
  2. Implementação do `ErrorResponseDTO` e `FieldErrorDTO` para formato uniforme de erro em toda a API.
  3. Adição da dependência `spring-boot-starter-validation` no `pom.xml`.
  4. Introdução de DTOs estritos: `UsuarioCadastroRequestDTO` e `UsuarioLoginRequestDTO` com Jakarta Bean Validation (`@NotBlank`, `@Email`, `@Size`).
  5. Criação do `UsuarioResponseDTO` com garantia absoluta de **não exposição de senha** em respostas JSON.
  6. Configuração de CORS por ambiente (`CorsConfig.java`) via `cors.allowed-origins`.
  7. Configuração de logs com mascaramento de parâmetros SQL no `application.properties`.
  8. Configuração de `Clock` injetável (`AppConfig.java`) para testes determinísticos de tempo.
  9. Criação de bateria de testes unitários (`GlobalExceptionHandlerTest.java`) validando HTTP 422, 404 e segurança de senhas.

---

### Fase 3 — Migrar Autenticação e Autorização ✅ [CONCLUÍDA]
* **Objetivo:** Eliminar confiança cega em IDs enviados pelo cliente, introduzir hash criptográfico BCrypt e RBAC rigoroso (ALUNO, PROFESSOR, ADMIN).
* **Entregas Realizadas:**
  1. Configuração do Spring Security 6 com `SecurityFilterChain` stateless, CSRF desabilitado e CORS unificado (`SecurityConfig.java`).
  2. Implementação do `BCryptPasswordEncoder` com cost factor 12 para hashing adaptativo seguro contra força bruta.
  3. Mapeamento de papéis no sistema via enum `PapelUsuario` (ROLE_ALUNO, ROLE_PROFESSOR, ROLE_ADMIN) integrado ao `UserDetails`.
  4. Criação da entidade e repositório `CodigoVerificacao` persistindo códigos no banco com expiração em 15 minutos (`f3-t9`).
  5. Implementação de `RateLimitingService` em memória protegendo login (5 tentativas/15m) e reenvio de código (3 tentativas/1h).
  6. Migration versionada `V2__auth_security_schema.sql` (Flyway) e `LegacyPasswordMigrationService` para upgrade automático de senhas legadas em texto puro.
  7. Provedor de JWT (`JwtTokenProvider`) emitindo Access Token (24h) e Refresh Token (7 dias), com DTOs tipados e endpoint `POST /api/auth/login`.
  8. Implementação de `TokenBlacklistService` em memória e endpoint `POST /api/auth/logout` para revogação instantânea de credenciais.
  9. Validação do flag `email_verificado` antes de conceder acesso às rotas autenticadas do sistema.
  10. Criação do `ResourceOwnershipService` para validação de posse do recurso (`validarPropriedadeUsuario`), eliminando vulnerabilidade de ID Spoofing.

---

### Fase 4 — Padronizar Contrato Mobile/API & OpenAPI 3.0 ✅ [CONCLUÍDA]
* **Objetivo:** Unificar contratos em `/api/v1`, documentação viva OpenAPI 3.0/Swagger, DTOs de agendamento e sincronização com TypeScript do mobile.
* **Entregas Realizadas:**
  1. **Prefixo Global e Versionamento (`f4-t1`):** Adotado `/api/v1` em todos os Controllers (`/api/v1/agendamentos`, `/api/v1/usuarios`, `/api/v1/auth`, `/api/v1/health`, `/api/v1/saberes`).
  2. **Verbos e Status Semânticos (`f4-t2`):** `POST` -> 201 Created com header e body; `GET/PUT/PATCH` -> 200 OK; Validação -> 400 Bad Request; Autenticação -> 401; Autorização -> 403; Conflito -> 409; Regra de Negócio -> 422.
  3. **Consolidação de Perfil (`f4-t3`):** Criado endpoint unificado `GET /api/v1/usuarios/{id}/perfil` (`PerfilCompletoDTO`) integrando dados cadastrais, saberes, aptidões e interesses numa só requisição.
  4. **Padronização de Rotas de Agendamento (`f4-t4`):** Rotas semânticas: `POST /api/v1/agendamentos`, `GET /api/v1/agendamentos/aluno/{id}`, `GET /api/v1/agendamentos/professor/{id}`, `PUT /{id}/aceitar`, `PUT /{id}/recusar`, `PUT /{id}/cancelar`, `PUT /{id}/remarcar`.
  5. **SpringDoc OpenAPI 3.0 & Swagger UI (`f4-t5`):** Criada classe `OpenApiConfig.java` com schema `bearerAuth` (JWT) e Swagger UI em `/swagger-ui.html` e `/api/v1/swagger-ui.html`.
  6. **DTOs de Agendamento (`f4-t6`):** Implementados `AgendamentoRequestDTO`, `AgendamentoResponseDTO`, `RemarcarAgendamentoDTO`, `CancelarAgendamentoDTO` e `RecusarAgendamentoDTO`.
  7. **Alinhamento de Enums (`f4-t7`):** Criado enum `StatusAgendamento` sincronizado com o type TypeScript do mobile.
  8. **Envelope Unificado (`ApiResponse<T>`):** Envelope padronizado contendo `success`, `message`, `data` e `timestamp`.

---
