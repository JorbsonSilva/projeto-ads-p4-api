# Fase 9 — Backend: Assistente Controlado por Regras (Triagem Determinística)

## Arquitetura e Decisões Técnicas
- **Zero Custo de API**: Elimina dependências de serviços pagos (OpenAI/Anthropic).
- **Sem Alucinação**: Todas as respostas são baseadas na máquina de estados finitos (`TriageStep`) e nos dados oficiais do banco.
- **Fail-Safe para Suporte**: Perguntas desconhecidas ou fora de escopo são roteadas para a fila de suporte humano com protocolo gerado automaticamente.
- **Apenas Leitura (@Transactional(readOnly = true))**: O assistente não possui permissão para gravar ou agendar aulas diretamente. O card de ação direciona o aluno para a tela padrão de confirmação.

## Endpoints Adicionados:
- `POST /api/v1/assistente/triagem` — Processamento de cada etapa da triagem
- `GET /api/v1/assistente/regras` — Especificação da árvore de decisão para auditoria
- `GET /api/v1/assistente/faq` — FAQ determinístico embutido
