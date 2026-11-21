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
