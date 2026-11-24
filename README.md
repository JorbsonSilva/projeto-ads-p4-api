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

### Cliente HTTP interno · ML-004B.2

Com a integração habilitada, `RagHttpClient` usa `RestClient` com um transporte
Apache HttpClient dedicado para criar e encerrar conversas, enviar mensagens e
consultar liveness e readiness em `/internal/v1`. Todas as operações exigem um
principal autenticado no Spring, inclusive as consultas de saúde. A resposta
preserva o modo do Python e distingue `answered` de `no_evidence`. O modo
`simulated` serve à integração e aos testes.

O cliente usa a URL e os timeouts da configuração acima. O transporte desativa
retries, redirecionamentos e cookies. O timeout de conexão e de obtenção de
conexão do pool começa em `500ms`. O timeout de espera pela resposta e de
inatividade da leitura começa em `3s`. O limite local é
`ORIENTA_RAG_MAX_CONCURRENT_REQUESTS=4` por processo. Chamadas além do limite
falham sem fila no cliente. Em múltiplas instâncias, o limite precisa ser ajustado
em relação à capacidade do Python.

Cada chamada emite o Bearer interno com o emissor da ML-004B.1. O corpo da
mensagem contém somente `messageId` e `question`. Um reenvio explícito dentro da
mesma conversa deve usar o mesmo UUID e conteúdo. Um timeout pode ocorrer após o
Python ter processado a operação.

As falhas são retornadas ao chamador Java como `RagClientException`, com código
tipado e indicação de reenvio. Elas distinguem autenticação interna, conversa
inexistente, conflito, limites, indisponibilidade, timeout, erro de conexão e
resposta inválida. O cliente descarta a mensagem bruta e valida IDs, estados,
evidências e campos obrigatórios. Os DTOs internos permanecem na camada de
integração. O controlador público, o mapeamento em `ApiResponse<T>` e eventual
contingência ficam para a próxima etapa.

Para executar os testes do cliente, incluindo transporte local e
interoperabilidade com o verificador Python, use o seguinte comando. Os dois
repositórios devem estar lado a lado no workspace. Os testes usam mocks e
servidores de loopback e dispensam pesos ou credenciais de provedores externos.

```sh
ORIENTA_RAG_TEST_PROJECT="$(cd ../projeto-ads-p4-rag && pwd)" \
ORIENTA_RAG_TEST_PYTHON=/caminho/venv/bin/python \
./mvnw -Dtest=RagHttpClientTest,RagHttpClientTransportTest,RagInternalTokenIssuerTest test
```

### API pública autenticada · ML-004B.3

Use o JWT de acesso do Orienta nas três rotas. O Spring identifica o usuário
pelo principal e envia ao Python uma credencial interna separada. A API Python
permanece restrita à rede privada.

| Endpoint | Entrada | Sucesso em `ApiResponse<T>` |
|---|---|---|
| `POST /api/v1/tutor/conversas` | `{}` | `201`, `conversaId` e `expiraEm` |
| `POST /api/v1/tutor/conversas/{conversaId}/mensagens` | `{"mensagemId":"<UUID>","pergunta":"Consigo trocar o horário da mentoria?"}` | `200`, IDs, `resultado`, `resposta`, `origem` e `evidencias` |
| `DELETE /api/v1/tutor/conversas/{conversaId}` | Sem corpo | `200`, `conversaId` confirmado pelo serviço interno |

`SEM_EVIDENCIA` é um resultado válido com evidências vazias. A origem
`SIMULADO` identifica respostas do FastAPI em modo simulado. Um reenvio deve
reutilizar `mensagemId` e o texto na mesma conversa. Conversas de outro usuário,
encerradas, expiradas ou perdidas após reinício devolvem o mesmo `404`. A
expiração informada na criação é inicial e o Python controla o TTL de inatividade.

Erros do tutor incluem `data.codigo` e `data.reenviavel`. Um `502` indica
falha de integração, inclusive de autenticação interna. Indisponibilidade
retorna `503`, e timeout retorna `504`. Os erros de autenticação do
aplicativo seguem a cadeia de segurança existente. Durante indisponibilidade,
o aplicativo pode apresentar a triagem como ação separada. O tutor não
dispara recomendações, cards ou contingência determinística automaticamente.

O teste de integração usa MockMvc com filtros reais e inicia o FastAPI existente
em processo separado, em uma porta efêmera de loopback. Ele envia solicitações
HTTP reais do Spring ao Python, no modo simulado, com segredos de teste gerados
localmente. Use um ambiente Python com `requirements-http.txt` do projeto RAG e
um ambiente que permita sockets de loopback. Sem `ORIENTA_RAG_HTTP_TEST_PYTHON`,
esse teste é ignorado. Com a variável definida, uma falha de inicialização é
reportada como falha de teste.

```sh
ORIENTA_RAG_TEST_PROJECT="$(cd ../projeto-ads-p4-rag && pwd)" \
ORIENTA_RAG_TEST_PYTHON=/caminho/venv/bin/python \
ORIENTA_RAG_HTTP_TEST_PYTHON=/caminho/venv/bin/python \
./mvnw test
```
