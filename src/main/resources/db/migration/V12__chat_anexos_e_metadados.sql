-- ==============================================================================
-- V12: EXTENSÃO DE MENSAGENS DO CHAT (ANEXOS, TIPOS E METADADOS)
-- ==============================================================================
-- Rastreabilidade: Fase 7 - Bloco K (BACK-014)
-- Envio e Recuperação de Mensagens com Status de Leitura (Duplo Tique e Anexos)
-- ==============================================================================

ALTER TABLE tb_mensagens ADD COLUMN IF NOT EXISTS tipo VARCHAR(30) DEFAULT 'TEXTO';
ALTER TABLE tb_mensagens ADD COLUMN IF NOT EXISTS anexo_url VARCHAR(1000);
ALTER TABLE tb_mensagens ADD COLUMN IF NOT EXISTS nome_arquivo VARCHAR(255);
ALTER TABLE tb_mensagens ADD COLUMN IF NOT EXISTS tamanho_arquivo VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_tb_mensagens_tipo ON tb_mensagens(tipo);
