-- ====================================================================
-- ORIENTA - MIGRATION V1: BASELINE SCHEMA DO LEGADO (P3 -> P4)
-- Rastreabilidade: DT-006, Plano de Migração
-- ====================================================================

-- 1. Tabela de Usuários
CREATE TABLE IF NOT EXISTS tb_usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    cpf VARCHAR(14) UNIQUE,
    data_nascimento VARCHAR(10),
    bio_aluno TEXT,
    bio_professor TEXT,
    foto_url VARCHAR(1000),
    nota_media NUMERIC(3,2) DEFAULT 5.0,
    total_avaliacoes INTEGER DEFAULT 0
);

-- 2. Tabela de Saberes (Disciplinas)
CREATE TABLE IF NOT EXISTS tb_saberes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    categoria VARCHAR(255) NOT NULL
);

-- 3. Tabela de Interesses (Aluno <-> Disciplina)
CREATE TABLE IF NOT EXISTS tb_interesses (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    saber_id BIGINT NOT NULL REFERENCES tb_saberes(id) ON DELETE CASCADE
);

-- 4. Tabela de Aptidões (Professor <-> Disciplina)
CREATE TABLE IF NOT EXISTS tb_aptidoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    saber_id BIGINT NOT NULL REFERENCES tb_saberes(id) ON DELETE CASCADE,
    preco_hora NUMERIC(10,2),
    nivel_dominio VARCHAR(50)
);

-- 5. Tabela de Disponibilidade (Professor)
CREATE TABLE IF NOT EXISTS tb_disponibilidade (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    dia_semana VARCHAR(30) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL
);

-- 6. Tabela de Agendamento (Legado Inicial)
CREATE TABLE IF NOT EXISTS tb_agendamento (
    id BIGSERIAL PRIMARY KEY,
    id_aluno BIGINT NOT NULL REFERENCES tb_usuarios(id),
    id_professor BIGINT NOT NULL REFERENCES tb_usuarios(id),
    data DATE NOT NULL,
    hora TIME NOT NULL,
    status VARCHAR(50) NOT NULL,
    justificativa_cancelamento VARCHAR(500)
);

-- Índices iniciais de performance e integridade
CREATE INDEX IF NOT EXISTS idx_tb_usuarios_email ON tb_usuarios(email);
CREATE INDEX IF NOT EXISTS idx_tb_agendamento_aluno ON tb_agendamento(id_aluno);
CREATE INDEX IF NOT EXISTS idx_tb_agendamento_professor ON tb_agendamento(id_professor);
