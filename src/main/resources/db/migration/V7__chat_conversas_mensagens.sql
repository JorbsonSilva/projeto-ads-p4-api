-- ====================================================================
-- ORIENTA - MIGRATION V7: CHAT INTERNO, CONVERSAS E MENSAGENS
-- Rastreabilidade: Fase 7 - f7-t1, f7-t2, f7-t3 / DT-006, DT-013
-- Restringe a comunicação exclusivamente ao aluno e professor da aula
-- Sem qualquer dependência de provedores pagos (custo zero)
-- ====================================================================

-- 1. Tabela de Conversa vinculada ao Agendamento
CREATE TABLE IF NOT EXISTS tb_conversas (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL UNIQUE REFERENCES tb_agendamento(id) ON DELETE CASCADE,
    ativa BOOLEAN DEFAULT TRUE,
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_conversas_agendamento ON tb_conversas(agendamento_id);

-- 2. Tabela de Mensagens
CREATE TABLE IF NOT EXISTS tb_mensagens (
    id BIGSERIAL PRIMARY KEY,
    conversa_id BIGINT NOT NULL REFERENCES tb_conversas(id) ON DELETE CASCADE,
    remetente_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    conteudo TEXT NOT NULL,
    lida BOOLEAN DEFAULT FALSE,
    lida_em TIMESTAMP,
    enviada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_mensagens_conversa ON tb_mensagens(conversa_id);
CREATE INDEX IF NOT EXISTS idx_tb_mensagens_remetente ON tb_mensagens(remetente_id);
CREATE INDEX IF NOT EXISTS idx_tb_mensagens_data ON tb_mensagens(enviada_em);
