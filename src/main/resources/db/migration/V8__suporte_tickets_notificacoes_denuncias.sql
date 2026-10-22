-- ====================================================================
-- ORIENTA - MIGRATION V8: SISTEMA DE SUPORTE, NOTIFICAÇÕES E DENÚNCIAS
-- Rastreabilidade: Fase 7 - f7-t4, f7-t5, f7-t7 / DT-006, DT-013
-- Permite tickets de atendimento, moderação de conduta e avisos internos
-- ====================================================================

-- 1. Tabela de Tickets de Suporte Interno
CREATE TABLE IF NOT EXISTS tb_tickets_suporte (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    protocolo VARCHAR(20) NOT NULL UNIQUE,
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    categoria VARCHAR(50) NOT NULL, -- DUVIDA, FINANCEIRO, PROBLEMA_TECNICO, CONDUTA, OUTRO
    prioridade VARCHAR(30) DEFAULT 'MEDIA', -- BAIXA, MEDIA, ALTA, URGENTE
    status VARCHAR(30) DEFAULT 'ABERTO', -- ABERTO, EM_ATENDIMENTO, AGUARDANDO_USUARIO, RESOLVIDO, FECHADO
    resposta TEXT,
    respondido_por_id BIGINT REFERENCES tb_usuarios(id),
    respondido_em TIMESTAMP,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_tickets_usuario ON tb_tickets_suporte(usuario_id);
CREATE INDEX IF NOT EXISTS idx_tb_tickets_status ON tb_tickets_suporte(status);
CREATE INDEX IF NOT EXISTS idx_tb_tickets_protocolo ON tb_tickets_suporte(protocolo);

-- 2. Tabela de Notificações Internas (sem serviços pagos / polling-friendly)
CREATE TABLE IF NOT EXISTS tb_notificacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    titulo VARCHAR(100) NOT NULL,
    mensagem TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL, -- AGENDAMENTO, CHAT, SUPORTE, SISTEMA, AVALIACAO
    lida BOOLEAN DEFAULT FALSE,
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    referencia_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_tb_notificacoes_usuario ON tb_notificacoes(usuario_id);
CREATE INDEX IF NOT EXISTS idx_tb_notificacoes_lida ON tb_notificacoes(usuario_id, lida);

-- 3. Tabela de Denúncias e Moderação de Conduta
CREATE TABLE IF NOT EXISTS tb_denuncias (
    id BIGSERIAL PRIMARY KEY,
    denunciante_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    denunciado_id BIGINT NOT NULL REFERENCES tb_usuarios(id),
    agendamento_id BIGINT REFERENCES tb_agendamento(id),
    motivo VARCHAR(100) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDENTE', -- PENDENTE, EM_ANALISE, PROCEDENTE, ARQUIVADA
    parecer_moderacao TEXT,
    analisado_por_id BIGINT REFERENCES tb_usuarios(id),
    criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    analisada_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_denuncias_denunciante ON tb_denuncias(denunciante_id);
CREATE INDEX IF NOT EXISTS idx_tb_denuncias_denunciado ON tb_denuncias(denunciado_id);
CREATE INDEX IF NOT EXISTS idx_tb_denuncias_status ON tb_denuncias(status);
