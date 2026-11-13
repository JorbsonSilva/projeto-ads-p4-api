-- ====================================================================
-- ORIENTA - MIGRATION V15: ADICIONAR COLUNA DOMINIO EM TB_AVALIACOES
-- Rastreabilidade: BACK-008
-- ====================================================================

ALTER TABLE tb_avaliacoes ADD COLUMN IF NOT EXISTS dominio INTEGER CHECK (dominio >= 1 AND dominio <= 5);
