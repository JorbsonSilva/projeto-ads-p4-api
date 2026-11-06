-- ==============================================================================
-- V14: CANAL DE DENÚNCIAS E OUVIDORIA COM PROTOCOLO AUDITÁVEL (DEN-YYYY-XXXXX)
-- ==============================================================================
-- Rastreabilidade: Fase 7 - Bloco K (BACK-016)
-- Expande a tabela tb_denuncias com protocolo único, vinculação de mensagem e parecer do moderador
-- ==============================================================================

ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS protocolo VARCHAR(50);
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS mensagem_id BIGINT REFERENCES tb_mensagens(id);
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS parecer_moderador TEXT;
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;

-- Criação de índice único para busca rápida por protocolo de ouvidoria
CREATE UNIQUE INDEX IF NOT EXISTS idx_tb_denuncias_protocolo ON tb_denuncias(protocolo);

-- Sincronizar dados existentes
UPDATE tb_denuncias SET parecer_moderador = parecer_moderacao WHERE parecer_moderador IS NULL AND parecer_moderacao IS NOT NULL;
UPDATE tb_denuncias SET data_atualizacao = criada_em WHERE data_atualizacao IS NULL;
