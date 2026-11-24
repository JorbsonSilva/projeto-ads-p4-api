# Orienta API — Fase 10: Testes, Ensaio e Entrega para a Banca

## 🎯 Objetivo da Fase 10
Garantir que a API esteja 100% testada, com script de reset de banco rápido (15 segundos) e dados representativos para demonstrar todo o ciclo de vida da plataforma durante a banca acadêmica de ADS.

## 🚀 Comandos Rápidos da Banca
1. Resetar banco com dados da banca:
   ```bash
   # No Linux/Mac:
   ./scripts/reset-banca-seed.sh

   # No Windows (PowerShell):
   ./scripts/reset-banca-seed.ps1
   ```

2. Executar testes de integração do fluxo completo:
   ```bash
   # Linux/macOS:
   ./mvnw test -Dtest=BancaFlowIntegrationTest

   # Windows (PowerShell):
   .\mvnw.cmd test -Dtest=BancaFlowIntegrationTest
   ```

3. Iniciar API local com dados sintéticos de desenvolvimento:
   ```bash
   # Primeiro, iniciar PostgreSQL e Mailpit locais:
   docker compose up -d postgres mailpit

   # Linux/macOS:
   ./mvnw spring-boot:run

   # Windows (PowerShell):
   .\mvnw.cmd spring-boot:run
   ```

O `spring-boot:run` ativa o perfil `dev` por padrão, que aplica uma migração Flyway para criar/verificar 500 contas sintéticas adicionais, cada uma com os papéis de aluno e professor, interesses, disciplinas ensinadas, preços e disponibilidades. O seed é idempotente e não roda ao executar o JAR fora desse comando. Cada clone recebe o mesmo conjunto em seu próprio banco local; isso não configura um banco remoto compartilhado.

Contas sintéticas locais usam o e-mail `demo.usuario.001@orienta.test` até `demo.usuario.500@orienta.test` e a senha de desenvolvimento `DemoLocal@2026!`. Use essas credenciais somente no ambiente local.

Contas dedicadas para capturas de tela e demonstrações (criadas junto com o perfil `dev`):
- **Aluno**: `demo.aluno@orienta.test` / `DemoLocal@2026!`
- **Professor**: `demo.professor@orienta.test` / `DemoLocal@2026!`

Essas contas têm histórico de aulas concluídas, confirmadas, pendentes, recusadas e remarcadas, além de conversas vinculadas a agendamentos com as contas sintéticas. Cada clone cria os dados no próprio banco local; essas credenciais não existem em produção nem em uma instância remota compartilhada.

## 👥 Contas Prontas para a Apresentação
- **Aluno**: `aluno.banca@orienta.edu.br` / `Senha@123`
- **Professor 1**: `prof.carlos@orienta.edu.br` / `Senha@123` (Banco de Dados, R$ 70,00/h, Centro)
- **Professor 2**: `profa.juliana@orienta.edu.br` / `Senha@123` (Cálculo I, R$ 55,00/h, Zona Sul)

## Tutor RAG interno · ML-004B.1

Os DTOs do tutor ficam em `com.equipe.projetoadsapi.tutor.dto` e usam o envelope `ApiResponse<T>`.
A regra de segurança exige autenticação em `/api/v1/tutor/**`. Esta etapa disponibiliza
a emissão da credencial interna para o futuro cliente Spring, ainda sem rotas de conversa.

| Variável de ambiente | Configuração |
|---|---|
| `ORIENTA_RAG_ENABLED` | `false` por padrão. Com `true`, valida a configuração antes de registrar o emissor. |
| `ORIENTA_RAG_BASE_URL` | `http://127.0.0.1:8000` por padrão. Use HTTPS fora de loopback. |
| `ORIENTA_RAG_ISSUER` e `ORIENTA_RAG_AUDIENCE` | `orienta-spring` e `orienta-rag`, conforme o contrato Python. |
| `ORIENTA_RAG_TOKEN_TTL` | `60s`, conforme o contrato desta etapa. |
| `ORIENTA_RAG_INTERNAL_SECRET` | Obrigatório quando ativo. Gere pelo menos 32 bytes UTF-8 para uso exclusivo do RAG e configure o mesmo valor em `RAG_INTERNAL_JWT_SECRET` no Python. |
| `ORIENTA_RAG_SUBJECT_SECRET` | Obrigatório quando ativo. Use outro segredo de pelo menos 32 bytes UTF-8, mantido apenas no Spring. |
| `ORIENTA_RAG_CONNECT_TIMEOUT` e `ORIENTA_RAG_RESPONSE_TIMEOUT` | `500ms` e `3s`. Reservados para o futuro cliente HTTP. |

O emissor usa o ID do `UsuarioDetails` autenticado para derivar via HMAC um sujeito
pseudônimo estável. O JWT interno usa HS256, audiência como string e escopo
`rag:internal`. A troca do segredo de pseudonimização muda o sujeito e invalida o acesso
a conversas associadas ao valor anterior. Mantenha ambos os segredos em variáveis
protegidas ou em um gerenciador de segredos e use valores distintos do segredo JWT
público. O Bearer recebido do aplicativo fica no Spring.

Para executar também o teste de interoperabilidade sem servidores, informe o
interpretador de um ambiente Python com PyJWT. Por padrão, o teste encontra o
protótipo em `../projeto-ads-p4-rag`. Use `ORIENTA_RAG_TEST_PROJECT` para outro caminho.

```sh
ORIENTA_RAG_TEST_PYTHON=/caminho/venv/bin/python ./mvnw -Dtest=RagInternalTokenIssuerTest test
```
