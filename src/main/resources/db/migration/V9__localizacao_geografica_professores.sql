-- ====================================================================
-- ORIENTA - MIGRATION V9: LOCALIZAÇÃO APROXIMADA E AULAS PRESENCIAIS
-- Rastreabilidade: Fase 8 (Localização Aproximada e Busca por Raio)
-- Tarefas: f8-t1, f8-t3, f8-t4, f8-t5 / MAP-001 a MAP-006 / LGPD MAP-SEC-002
-- ====================================================================

-- 1. Campos de localização aproximada e suporte presencial no Perfil do Usuário/Professor
ALTER TABLE tb_usuarios
    ADD COLUMN IF NOT EXISTS oferece_presencial BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS regiao_atendimento VARCHAR(100),
    ADD COLUMN IF NOT EXISTS latitude_aproximada NUMERIC(10, 6),
    ADD COLUMN IF NOT EXISTS longitude_aproximada NUMERIC(10, 6),
    ADD COLUMN IF NOT EXISTS raio_atendimento_km INTEGER DEFAULT 10;

-- 2. Garantir coluna de referência de localidade no agendamento (MAP-007)
ALTER TABLE tb_agendamento
    ADD COLUMN IF NOT EXISTS regiao_ou_local_referencia VARCHAR(255);

-- 3. Índices para performance em busca por modalidade e região
CREATE INDEX IF NOT EXISTS idx_tb_usuarios_presencial_regiao 
    ON tb_usuarios(oferece_presencial, regiao_atendimento);

-- 4. Seed de Localização e Modalidade Presencial para Professores de Teste da Banca
-- Coordenadas de referência aproximadas (centroides de bairro, sem expor residência exata)
UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Boa Viagem', 
    latitude_aproximada = -8.118900, 
    longitude_aproximada = -34.898800, 
    raio_atendimento_km = 10
WHERE email = 'helena.souza@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Graças / Espinheiro', 
    latitude_aproximada = -8.041600, 
    longitude_aproximada = -34.897400, 
    raio_atendimento_km = 5
WHERE email = 'marcos.andrade@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Várzea / Cidade Universitária', 
    latitude_aproximada = -8.053890, 
    longitude_aproximada = -34.951110, 
    raio_atendimento_km = 15
WHERE email = 'camila.barreto@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Derby / Boa Vista', 
    latitude_aproximada = -8.058300, 
    longitude_aproximada = -34.895000, 
    raio_atendimento_km = 8
WHERE email = 'rafael.duarte@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Casa Forte', 
    latitude_aproximada = -8.026700, 
    longitude_aproximada = -34.919700, 
    raio_atendimento_km = 10
WHERE email = 'fernanda.lima@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = false, 
    regiao_atendimento = NULL, 
    latitude_aproximada = NULL, 
    longitude_aproximada = NULL, 
    raio_atendimento_km = NULL
WHERE email = 'thiago.meireles@orienta.com.br'; -- Professor exclusivamente online

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Pina', 
    latitude_aproximada = -8.090100, 
    longitude_aproximada = -34.887200, 
    raio_atendimento_km = 5
WHERE email = 'juliana.rocha@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = false, 
    regiao_atendimento = NULL, 
    latitude_aproximada = NULL, 
    longitude_aproximada = NULL, 
    raio_atendimento_km = NULL
WHERE email = 'lucas.silveira@orienta.com.br'; -- Professor exclusivamente online

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Madalena', 
    latitude_aproximada = -8.056000, 
    longitude_aproximada = -34.911000, 
    raio_atendimento_km = 5
WHERE email = 'beatriz.monteiro@orienta.com.br';

UPDATE tb_usuarios 
SET oferece_presencial = true, 
    regiao_atendimento = 'Santo Amaro', 
    latitude_aproximada = -8.047500, 
    longitude_aproximada = -34.877000, 
    raio_atendimento_km = 10
WHERE email = 'diego.vasconcelos@orienta.com.br';
