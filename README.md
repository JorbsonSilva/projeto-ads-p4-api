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
   mvn test -Dtest=BancaFlowIntegrationTest
   ```

3. Iniciar API local em modo apresentação:
   ```bash
   mvn spring-boot:run
   ```

## 👥 Contas Prontas para a Apresentação
- **Aluno**: `aluno.banca@orienta.edu.br` / `Senha@123`
- **Professor 1**: `prof.carlos@orienta.edu.br` / `Senha@123` (Banco de Dados, R$ 70,00/h, Centro)
- **Professor 2**: `profa.juliana@orienta.edu.br` / `Senha@123` (Cálculo I, R$ 55,00/h, Zona Sul)
