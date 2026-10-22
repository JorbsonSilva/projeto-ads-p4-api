-- ====================================================================
-- ORIENTA - MIGRATION V3: EVOLUÇÃO DO DOMÍNIO DE AGENDAMENTOS
-- Rastreabilidade: Fase 5 (Evoluir Modelo de Dados e Agendamento)
-- Tarefas: f5-t3, f5-t4, f5-t5, f5-t7 / DT-006, DT-013
-- ====================================================================

-- 1. Evolução da tabela de Agendamentos (tb_agendamento)
-- Adiciona vínculos de disciplina, modalidade, congelamento de preço e assunto
ALTER TABLE tb_agendamento
    ADD COLUMN IF NOT EXISTS saber_id BIGINT REFERENCES tb_saberes(id),
    ADD COLUMN IF NOT EXISTS modalidade VARCHAR(30) DEFAULT 'PRESENCIAL',
    ADD COLUMN IF NOT EXISTS valor_aplicado NUMERIC(10,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS assunto VARCHAR(255),
    ADD COLUMN IF NOT EXISTS criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. Tabela de Auditoria e Histórico de Transições de Status (f5-t7)
CREATE TABLE IF NOT EXISTS tb_agendamento_historico (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL REFERENCES tb_agendamento(id) ON DELETE CASCADE,
    status_anterior VARCHAR(30),
    status_novo VARCHAR(30) NOT NULL,
    motivo TEXT,
    alterado_por_id BIGINT REFERENCES tb_usuarios(id),
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices de integridade e consultas rápidas
CREATE INDEX IF NOT EXISTS idx_tb_agendamento_historico_agendamento 
    ON tb_agendamento_historico(agendamento_id);

CREATE INDEX IF NOT EXISTS idx_tb_agendamento_conflito 
    ON tb_agendamento(id_professor, data, hora, status);
