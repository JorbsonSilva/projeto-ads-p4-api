-- ==============================================================================
-- SEED DA BANCA — SISTEMA ORIENTA (FASE 10)
-- Contas, matérias, avaliações e histórico prontos para demonstração
-- Senha padrão para todos os usuários: Senha@123 (BCrypt hash)
-- ==============================================================================

-- 1. Limpeza segura
TRUNCATE TABLE tb_mensagens_chat CASCADE;
TRUNCATE TABLE tb_avaliacoes CASCADE;
TRUNCATE TABLE tb_aulas CASCADE;
TRUNCATE TABLE tb_professores_disciplinas CASCADE;
TRUNCATE TABLE tb_disciplinas CASCADE;
TRUNCATE TABLE tb_suporte_tickets CASCADE;
TRUNCATE TABLE tb_usuarios CASCADE;

-- 2. Cadastro dos Usuários
-- Senha: Senha@123 -> $2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO
INSERT INTO tb_usuarios (id, nome, email, senha_hash, papel, status, criado_em) VALUES
(1001, 'Gabriel Aluno (Banca)', 'aluno.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ALUNO', 'ATIVO', NOW()),
(1002, 'Prof. Dr. Carlos Eduardo', 'prof.carlos@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ATIVO', NOW()),
(1003, 'Profa. Ma. Juliana Oliveira', 'profa.juliana@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ATIVO', NOW()),
(1004, 'Mariana Coordenadora (Admin)', 'admin.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ADMIN', 'ATIVO', NOW());

-- 3. Perfil detalhado dos Professores (Fase 8: Raio e Localização)
INSERT INTO tb_professores_perfil (usuario_id, biografia, valor_hora, latitude_aprox, longitude_aprox, regiao_bairro, modalidade_atendimento) VALUES
(1002, 'Doutor em Ciência da Computação (UFSCar). Mais de 10 anos de experiência em Bancos Relacionais e Big Data.', 70.00, -23.550520, -46.633308, 'Centro / Bela Vista', 'AMBAS'),
(1003, 'Mestre em Matemática Pura (USP). Especialista no desbloqueio de alunos em Cálculo I e Álgebra Linear.', 55.00, -23.600000, -46.660000, 'Zona Sul / Moema', 'ONLINE');

-- 4. Disciplinas do Catálogo
INSERT INTO tb_disciplinas (id, nome, area, descricao) VALUES
(201, 'Cálculo I', 'Exatas', 'Limites, Derivadas, Regra da Cadeia e Aplicações'),
(202, 'Banco de Dados', 'Tecnologia', 'Modelagem Relacional, Normalização, SQL e ACID'),
(203, 'Estrutura de Dados', 'Tecnologia', 'Vetores, Listas, Árvores Binárias e Grafos');

-- 5. Vínculo Professor - Disciplinas
INSERT INTO tb_professores_disciplinas (professor_id, disciplina_id) VALUES
(1002, 202),
(1002, 203),
(1003, 201);

-- 6. Aulas para Demonstração da Banca
-- Aula 1: CONCLUIDA com Avaliação (mostrar histórico do aluno e nota média)
INSERT INTO tb_aulas (id, aluno_id, professor_id, disciplina_id, data_hora, duracao_minutos, status, valor, modalidade) VALUES
(301, 1001, 1002, 202, NOW() - INTERVAL '3 days', 60, 'CONCLUIDA', 70.00, 'ONLINE');

INSERT INTO tb_avaliacoes (id, aula_id, nota, comentario, criado_em) VALUES
(401, 301, 5, 'Excelente didática! Consegui entender normalização em menos de uma aula.', NOW() - INTERVAL '3 days');

-- Aula 2: AGENDADA para hoje (mostrar na tela inicial do aluno e do professor)
INSERT INTO tb_aulas (id, aluno_id, professor_id, disciplina_id, data_hora, duracao_minutos, status, valor, modalidade) VALUES
(302, 1001, 1003, 201, NOW() + INTERVAL '2 hours', 60, 'CONFIRMADA', 55.00, 'ONLINE');
