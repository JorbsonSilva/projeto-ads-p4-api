-- ====================================================================
-- ORIENTA - MIGRATION V5: SESSÃO DE AULA, CRONÔMETRO BILATERAL E EVENTOS
-- Rastreabilidade: Fase 6 - f6-t1, f6-t5 / DT-006, DT-013
-- Permite check-in mútuo, cronometragem auditada no servidor e eventos ao vivo.
-- ====================================================================

-- 1. Tabela de Sessão de Aula em Tempo Real
CREATE TABLE IF NOT EXISTS tb_sessao_aula (
    id BIGSERIAL PRIMARY KEY,
    agendamento_id BIGINT NOT NULL UNIQUE REFERENCES tb_agendamento(id) ON DELETE CASCADE,
    aluno_checkin BOOLEAN DEFAULT FALSE,
    professor_checkin BOOLEAN DEFAULT FALSE,
    aluno_checkin_em TIMESTAMP,
    professor_checkin_em TIMESTAMP,
    status_sessao VARCHAR(30) DEFAULT 'AGUARDANDO_CHECKIN', -- AGUARDANDO_CHECKIN, EM_ANDAMENTO, PAUSADA, AGUARDANDO_CONFIRMACAO, CONCLUIDA
    inicio_efetivo TIMESTAMP,
    fim_efetivo TIMESTAMP,
    segundos_pausados INTEGER DEFAULT 0,
    ultima_pausa_em TIMESTAMP,
    duracao_total_segundos INTEGER DEFAULT 0,
    aluno_confirmou_fim BOOLEAN DEFAULT FALSE,
    professor_confirmou_fim BOOLEAN DEFAULT FALSE,
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_sessao_agendamento ON tb_sessao_aula(agendamento_id);
CREATE INDEX IF NOT EXISTS idx_tb_sessao_status ON tb_sessao_aula(status_sessao);

-- 2. Tabela de Auditoria de Eventos de Aula (f6-t5)
CREATE TABLE IF NOT EXISTS tb_aula_eventos (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL REFERENCES tb_sessao_aula(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(50) NOT NULL, -- CHECKIN, INICIO, PAUSA, RETOMADA, SOLICITAR_FIM, CONFIRMAR_FIM
    autor_id BIGINT REFERENCES tb_usuarios(id),
    timestamp_servidor TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo VARCHAR(255),
    segundos_decorridos INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tb_aula_eventos_sessao ON tb_aula_eventos(sessao_id);
