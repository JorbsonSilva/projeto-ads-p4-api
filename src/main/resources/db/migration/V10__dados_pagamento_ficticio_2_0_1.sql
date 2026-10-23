-- =========================================================================
-- V10__dados_pagamento_ficticio_2_0_1.sql
-- Adiciona suporte a armazenamento demonstrativo de cartão de crédito
-- em conformidade com a Decisão Técnica 18 da Baseline 2.0.1 (PROP-FIN-01):
-- - Armazena estritamente a bandeira e os 4 últimos dígitos
-- - Proibição absoluta de persistência de código de segurança (CVV)
-- - Sem intermediação ou liquidação bancária real (fins acadêmicos)
-- =========================================================================

ALTER TABLE tb_usuarios
    ADD COLUMN IF NOT EXISTS cartao_ficticio_bandeira VARCHAR(30),
    ADD COLUMN IF NOT EXISTS cartao_ficticio_ultimos_digitos VARCHAR(4),
    ADD COLUMN IF NOT EXISTS cartao_ficticio_titular VARCHAR(100);

-- Comentários de auditoria no catálogo de metadados
COMMENT ON COLUMN tb_usuarios.cartao_ficticio_bandeira IS 'Bandeira do cartão para demonstração (ex: Mastercard, Visa)';
COMMENT ON COLUMN tb_usuarios.cartao_ficticio_ultimos_digitos IS 'Apenas os 4 últimos dígitos do cartão mascarado';
COMMENT ON COLUMN tb_usuarios.cartao_ficticio_titular IS 'Nome do titular para fins de exibição na tela de confirmação';

-- Atualizar contas de seed de demonstração com cartões fictícios de exemplo
UPDATE tb_usuarios 
SET cartao_ficticio_bandeira = 'Mastercard',
    cartao_ficticio_ultimos_digitos = '4242',
    cartao_ficticio_titular = 'ALUNO DEMONSTRACAO'
WHERE id = 11;
