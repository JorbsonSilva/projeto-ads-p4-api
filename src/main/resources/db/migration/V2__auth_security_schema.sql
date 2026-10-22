-- ====================================================================
-- ORIENTA - MIGRATION V2: AUTENTICAÇÃO, RBAC E PERSISTÊNCIA DE CÓDIGOS
-- Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t6, f3-t9, f3-t10 / DT-008
-- ====================================================================

-- 1. Adicionar colunas de Papel (RBAC) e Status de Verificação de E-mail
ALTER TABLE tb_usuarios 
    ADD COLUMN IF NOT EXISTS papel VARCHAR(30) DEFAULT 'ROLE_ALUNO',
    ADD COLUMN IF NOT EXISTS email_verificado BOOLEAN DEFAULT FALSE;

-- 2. Tabela de Códigos de Verificação Persistidos
CREATE TABLE IF NOT EXISTS tb_codigos_verificacao (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    utilizado BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_codigos_email_tipo ON tb_codigos_verificacao(email, tipo);

-- 3. Migração de Papéis para Contas Legadas
UPDATE tb_usuarios 
SET papel = 'ROLE_PROFESSOR' 
WHERE (papel IS NULL OR papel = 'ROLE_ALUNO') 
  AND (UPPER(COALESCE(tipo, '')) LIKE '%PROFESSOR%' OR bio_professor IS NOT NULL);

UPDATE tb_usuarios 
SET papel = 'ROLE_ADMIN' 
WHERE UPPER(COALESCE(tipo, '')) LIKE '%ADMIN%';

UPDATE tb_usuarios 
SET papel = 'ROLE_ALUNO' 
WHERE papel IS NULL;

-- 4. Normalizar contas legadas pré-existentes como já verificadas
UPDATE tb_usuarios 
SET email_verificado = TRUE 
WHERE email_verificado IS NULL OR email_verificado = FALSE;
