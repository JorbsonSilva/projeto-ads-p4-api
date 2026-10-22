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

### Fase 5 — Evoluir Modelo de Dados e Agendamento ✅ [CONCLUÍDA]
* **Objetivo:** Flyway migrations V3 e V4, regras de antecedência de 24h, congelamento de preço, concorrência atômica e auditoria de transições.
* **Entregas Realizadas:**
  1. **Migration V3 (`f5-t3`, `f5-t4`, `f5-t5`):** `V3__agendamento_domain_evolution.sql` vinculando `saber_id` (disciplina), `assunto`, `modalidade` (ONLINE/PRESENCIAL) e `valor_aplicado` (NUMERIC congelado).
  2. **Máquina de Estados & Enums (`f5-t6`):** Enum `StatusAgendamento` com transições permitidas (PENDENTE -> CONFIRMADO -> EM_ANDAMENTO -> AGUARDANDO_CONFIRMACAO -> REALIZADA; ou RECUSADO/CANCELADO).
  3. **Tabela de Histórico e Auditoria (`f5-t7`):** Criação da tabela `tb_agendamento_historico` com entidade JPA `AgendamentoHistorico`, repositório e endpoint `GET /api/v1/agendamentos/{id}/historico` registrando cada transição com data, hora, motivo e autor.
  4. **Antecedência Mínima de 24h no Servidor (`f5-t8`):** Validação estrita em `AgendamentoService` rejeitando aulas com menos de 24 horas de antecedência com HTTP 422 (`RegraNegocioException`).
  5. **Concorrência Atômica Transacional (`f5-t9`):** Anotação `@Transactional` e verificação no banco contra choques de horário considerando apenas agendamentos ativos (`existsByProfessorAndDataAndHoraAndStatusNotIn`).
  6. **Separação de Recusa e Cancelamento (`f5-t10`):** Recusa exclusiva para o professor em status `PENDENTE`; Cancelamento para aluno/professor/admin em `PENDENTE` ou `CONFIRMADO`.
  7. **Massa de Dados de Demonstração Seed (`f5-t11`):** Migration `V4__seed_professores_e_aulas.sql` gerando 10 professores reais com fotos, saberes, aptidões com preço, alunos e 20 agendamentos com status variados.

---

### Fase 6 — Implementar Aula, Cronômetro e Avaliação ✅ [CONCLUÍDA]
* **Objetivo:** Completar o ciclo da aula: início bilateral, cronometragem no servidor, consentimento mútuo e avaliação pós-aula (1 a 5 estrelas).
* **Entregas Realizadas:**
  1. **Migration V5 & Entidade SessaoAula (`f6-t1`):** Criação da tabela `tb_sessao_aula` com colunas `aluno_checkin`, `professor_checkin`, `inicio_efetivo`, `fim_efetivo`, `segundos_pausados`, `ultima_pausa_em`, `duracao_total_segundos` e status (`StatusSessao`).
  2. **Início Bilateral da Aula (`f6-t2`):** Check-in independente de cada participante; o cronômetro oficial só dispara e o agendamento muda para `EM_ANDAMENTO` após a confirmação mútua de aluno e professor.
  3. **Relógio do Servidor & Anti-Fraude (`f6-t3`):** Cálculo de segundos decorridos e pausas executado com precisão no servidor (`calcularSegundosDecorridos`), impossibilitando fraudes em relógios locais.
  4. **Pausa e Retomada por Consentimento Mútuo (`f6-t4`):** Endpoint para pausar a aula em andamento, congelando o tempo oficial com carimbo de `ultima_pausa_em` e somando intervalos pausados na retomada.
  5. **Tabela de Auditoria de Eventos de Aula (`f6-t5`):** Criação da tabela `tb_aula_eventos` e entidade `AulaEvento` registrando cada etapa do ciclo de vida ao vivo (`CHECKIN_ALUNO`, `CHECKIN_PROFESSOR`, `INICIO_AULA`, `PAUSA`, `RETOMADA`, `SOLICITACAO_FIM`, `CONFIRMACAO_FIM`).
  6. **Finalização Bilateral & Estado AGUARDANDO_CONFIRMACAO (`f6-t6`):** O primeiro encerramento transita para `AGUARDANDO_CONFIRMACAO`; apenas após o aceite mútuo a aula atinge `REALIZADA` e a sessão `CONCLUIDA`.
  7. **Módulo de Avaliação Pós-Aula & Migration V6 (`f6-t8`):** Criação da tabela `tb_avaliacoes` e entidade `Avaliacao`, com validação de nota de 1 a 5 estrelas, pontualidade, didática e comentário. Bloqueio estrito de avaliações antes do status `REALIZADA` e cálculo dinâmico da média de notas do usuário avaliado.
  8. **Testes Unitários da Máquina de Estados (`f6-t9`):** Bateria de testes em `MaquinaEstadosAulaTest.java` cobrindo check-in bilateral, transições ilegais, pausas inválidas, fluxo de encerramento e bloqueio de intrusos.
  9. **Endpoints do Controller (`AulaController.java`):** Métodos REST anotados com OpenAPI 3.0 e validação de posse de recurso (`ResourceOwnershipService`).

---
