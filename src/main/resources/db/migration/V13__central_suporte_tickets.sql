-- ==============================================================================
-- V13: CENTRAL DE SUPORTE, PROTOCOLO AUDITÁVEL (SUP-YYYY-XXXXX) E RESPOSTAS
-- ==============================================================================
-- Rastreabilidade: Fase 7 - Bloco K (BACK-015)
-- Expande tamanho de protocolo para 50 caracteres e adiciona compatibilidade com novos campos
-- ==============================================================================

ALTER TABLE tb_tickets_suporte ALTER COLUMN protocolo TYPE VARCHAR(50);
ALTER TABLE tb_tickets_suporte ADD COLUMN IF NOT EXISTS assunto VARCHAR(150);
ALTER TABLE tb_tickets_suporte ADD COLUMN IF NOT EXISTS resposta_admin TEXT;

-- Sincronizar dados existentes
UPDATE tb_tickets_suporte SET assunto = titulo WHERE assunto IS NULL AND titulo IS NOT NULL;
UPDATE tb_tickets_suporte SET resposta_admin = resposta WHERE resposta_admin IS NULL AND resposta IS NOT NULL;
