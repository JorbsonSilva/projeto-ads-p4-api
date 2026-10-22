-- ====================================================================
-- ORIENTA - MIGRATION V6: AVALIAÇÃO DE AULAS E FEEDBACK
-- Rastreabilidade: Fase 6 - f6-t8 / DT-006, DT-013
-- Avaliação só pode ocorrer após a aula atingir status REALIZADA
-- ====================================================================

CREATE TABLE IF NOT EXISTS tb_avaliacoes (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL REFERENCES tb_agendamento(id) ON DELETE CASCADE,
    avaliador_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    avaliado_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    nota INTEGER NOT NULL CHECK (nota >= 1 AND nota <= 5),
    pontualidade INTEGER CHECK (pontualidade >= 1 AND pontualidade <= 5),
    didatica INTEGER CHECK (didatica >= 1 AND didatica <= 5),
    comentario TEXT,
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_avaliacao_agendamento_avaliador UNIQUE (agendamento_id, avaliador_id)
);

CREATE INDEX IF NOT EXISTS idx_tb_avaliacoes_avaliado ON tb_avaliacoes(avaliado_id);
CREATE INDEX IF NOT EXISTS idx_tb_avaliacoes_agendamento ON tb_avaliacoes(agendamento_id);
