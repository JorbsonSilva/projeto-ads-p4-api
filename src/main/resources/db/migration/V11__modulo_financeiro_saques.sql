-- ==============================================================================
-- V11: MÓDULO FINANCEIRO, SALDOS, TRANSAÇÕES E SOLICITAÇÕES DE SAQUE
-- ==============================================================================
-- Rastreabilidade: Fase 7 - Bloco J (BACK-009)
-- Telas frontend associadas: Tela de Saque (IMG-010 / saque.tsx), Painel de Ganhos (IMG-013 / painel-ganhos.tsx)
-- Regras de negócio: Taxa de retenção institucional (10%), chaves PIX, protocolos SAQ-YYYY-XXXXX
-- ==============================================================================

-- 1. Tabela de Solicitações de Saque do Professor
CREATE TABLE IF NOT EXISTS tb_saque (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    taxa_aplicada DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    valor_liquido DECIMAL(10,2) NOT NULL,
    status_saque VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    tipo_chave_pix VARCHAR(30),
    chave_pix VARCHAR(255),
    dados_bancarios TEXT,
    protocolo_saque VARCHAR(50) NOT NULL UNIQUE,
    data_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_processamento TIMESTAMP,
    observacoes TEXT,
    CONSTRAINT fk_saque_professor FOREIGN KEY (professor_id) REFERENCES tb_usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT chk_saque_status CHECK (status_saque IN ('PENDENTE', 'PROCESSANDO', 'CONCLUIDO', 'RECUSADO')),
    CONSTRAINT chk_saque_tipo_chave_pix CHECK (tipo_chave_pix IS NULL OR tipo_chave_pix IN ('CPF', 'EMAIL', 'TELEFONE', 'EVP')),
    CONSTRAINT chk_saque_valores CHECK (valor > 0 AND valor_liquido >= 0)
);

CREATE INDEX IF NOT EXISTS idx_tb_saque_professor ON tb_saque(professor_id);
CREATE INDEX IF NOT EXISTS idx_tb_saque_status ON tb_saque(status_saque);
CREATE INDEX IF NOT EXISTS idx_tb_saque_data ON tb_saque(data_solicitacao DESC);
CREATE INDEX IF NOT EXISTS idx_tb_saque_protocolo ON tb_saque(protocolo_saque);

-- 2. Tabela de Transações Financeiras (Extrato Cronológico e Livro Razão)
CREATE TABLE IF NOT EXISTS tb_transacao_financeira (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    agendamento_id BIGINT,
    saque_id BIGINT,
    tipo_transacao VARCHAR(50) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    descricao VARCHAR(255),
    data_transacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transacao_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id) ON DELETE RESTRICT,
    CONSTRAINT fk_transacao_agendamento FOREIGN KEY (agendamento_id) REFERENCES tb_agendamento(id) ON DELETE SET NULL,
    CONSTRAINT fk_transacao_saque FOREIGN KEY (saque_id) REFERENCES tb_saque(id) ON DELETE SET NULL,
    CONSTRAINT chk_tipo_transacao CHECK (tipo_transacao IN ('CREDITO_AULA', 'RETENCAO_TAXA_INSTITUICAO', 'SAQUE_DEBITO', 'REEMBOLSO_ALUNO'))
);

CREATE INDEX IF NOT EXISTS idx_tb_transacao_usuario ON tb_transacao_financeira(usuario_id);
CREATE INDEX IF NOT EXISTS idx_tb_transacao_agendamento ON tb_transacao_financeira(agendamento_id);
CREATE INDEX IF NOT EXISTS idx_tb_transacao_saque ON tb_transacao_financeira(saque_id);
CREATE INDEX IF NOT EXISTS idx_tb_transacao_data ON tb_transacao_financeira(data_transacao DESC);
CREATE INDEX IF NOT EXISTS idx_tb_transacao_tipo ON tb_transacao_financeira(tipo_transacao);
