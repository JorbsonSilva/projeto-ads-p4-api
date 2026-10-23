# 📝 Relatório de Defeitos Abertos e Limitações Conhecidas (Fase 10)

> *"Na engenharia de software real, reconhecer as limitações técnicas e justificar os trade-offs arquiteturais demonstra maturidade superior a alegar um sistema perfeito."*

## 1. Limitações de Arquitetura por Design (Trade-offs Justificados)

### 1.1. Assistente Determinístico vs Modelo Generativo
- **Limitação**: O assistente não responde a perguntas de bate-papo livre filosófico ou poesia.
- **Justificativa**: Evitar alucinações de preços, cancelamentos acidentais e custos de US$ 0.03 por requisição.
- **Tratamento**: Mensagens fora do catálogo são roteadas diretamente para a fila de suporte humano (`tb_suporte_tickets`).

### 1.2. Cálculo de Distância Euclidiana/Haversine vs Rota de Trânsito Real
- **Limitação**: O cálculo considera a distância linear em linha reta (raio em km) e não a rota em tempo real de trânsito.
- **Justificativa**: Eliminar a dependência da Google Directions API (que exige cartão de crédito e faturamento em dólar).
- **Tratamento**: O cálculo de Haversine é nativo e executado em microssegundos no Postgres/Java.

### 1.3. Notificações Push Locais vs Firebase Cloud Messaging Corporativo
- **Limitação**: Em modo de demonstração local, as notificações utilizam o sistema local do dispositivo.
- **Justificativa**: Evita necessidade de chaves de serviço do Google atreladas a uma conta empresarial paga.

## 2. Checklist de Integridade
- [x] Zero credenciais ou chaves secretas versionadas no repositório.
- [x] Senhas salvas exclusivamente com hash BCrypt com salt de 10 rounds.
- [x] Todas as rotas de negócio protegidas por token JWT com expiração.
- [x] Todas as tabelas versionadas via Flyway com histórico imutável.
