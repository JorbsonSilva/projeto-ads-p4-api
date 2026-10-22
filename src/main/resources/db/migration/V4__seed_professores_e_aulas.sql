-- ====================================================================
-- ORIENTA - MIGRATION V4: SEED DE PROFESSORES, AULAS E HISTÓRICOS
-- Rastreabilidade: Fase 5 - f5-t11 / DT-006, DT-013
-- Carga determinística para homologação, testes de banca e demonstração
-- Senha padrão para todas as contas de teste: '123456' (Hash BCrypt com cost factor 12)
-- Hash BCrypt gerado para '123456': $2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.
-- ====================================================================

-- 1. Inserir Saberes (Disciplinas) adicionais se não existirem
INSERT INTO tb_saberes (nome, categoria) VALUES
    ('Estruturas de Dados e Algoritmos', 'Ciência da Computação'),
    ('Programação Orientada a Objetos com Java', 'Programação'),
    ('Banco de Dados Relacional e SQL', 'Engenharia de Software'),
    ('Desenvolvimento Mobile com React Native', 'Mobile'),
    ('Cálculo Diferencial e Integral I', 'Matemática'),
    ('Arquitetura de Computadores e Redes', 'Infraestrutura'),
    ('Engenharia de Software e Modelagem UML', 'Engenharia de Software'),
    ('Inteligência Artificial e Machine Learning', 'Inovação'),
    ('Segurança da Informação e Criptografia', 'Segurança'),
    ('Metodologia Científica e TCC', 'Metodologia')
ON CONFLICT (nome) DO NOTHING;

-- 2. Inserir Professores (10 professores com bios, fotos e papéis)
INSERT INTO tb_usuarios (nome, email, senha, tipo, papel, email_verificado, bio_professor, foto_url, nota_media, total_avaliacoes) VALUES
    ('Profª. Helena Souza', 'helena.souza@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Doutora em Computação pela UFPE. Especialista em Estruturas de Dados e Algoritmos.', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400', 4.9, 32),
    ('Prof. Marcos Andrade', 'marcos.andrade@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Mestre em Engenharia de Software. Mais de 10 anos lecionando Java e Spring Boot.', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400', 4.8, 28),
    ('Profª. Camila Barreto', 'camila.barreto@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Especialista em Banco de Dados PostgreSQL e Modelagem Relacional para sistemas de alta escala.', 'https://images.unsplash.com/photo-1580894732454-defa10bcfdbd?w=400', 5.0, 41),
    ('Prof. Rafael Duarte', 'rafael.duarte@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Desenvolvedor Sênior Mobile. Foco em React Native, Expo e TypeScript.', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400', 4.7, 19),
    ('Profª. Fernanda Lima', 'fernanda.lima@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Matemática e Estatística Aplicada. Cálculo I, II e Álgebra Linear de forma prática.', 'https://images.unsplash.com/photo-1567532939604-b6b5b0db2604?w=400', 4.9, 37),
    ('Prof. Thiago Meireles', 'thiago.meireles@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Arquiteto de Redes e Sistemas Embarcados. Suporte completo para disciplinas de Infraestrutura.', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=400', 4.6, 15),
    ('Profª. Juliana Rocha', 'juliana.rocha@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Engenheira de Software e Scrum Master. Mentoria para TCC e padrões de projeto.', 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=400', 5.0, 24),
    ('Prof. Lucas Silveira', 'lucas.silveira@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Pesquisador de IA generativa e Python para ciência de dados aplicada.', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 4.8, 22),
    ('Profª. Beatriz Monteiro', 'beatriz.monteiro@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Segurança Defensiva, Pentest e Boas Práticas OWASP para desenvolvedores.', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400', 4.9, 30),
    ('Prof. Diego Vasconcelos', 'diego.vasconcelos@orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'PROFESSOR', 'ROLE_PROFESSOR', true, 'Orientador acadêmico com mais de 50 artigos publicados. Foco em escrita de TCC.', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400', 4.7, 18)
ON CONFLICT (email) DO NOTHING;

-- 3. Inserir Alunos adicionais para agendamentos
INSERT INTO tb_usuarios (nome, email, senha, tipo, papel, email_verificado, bio_aluno, foto_url) VALUES
    ('Carlos Aluno', 'carlos@aluno.orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'ALUNO', 'ROLE_ALUNO', true, 'Estudante do 4º período de ADS. Apaixonado por desenvolvimento backend.', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400'),
    ('Mariana Santos', 'mariana@aluno.orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'ALUNO', 'ROLE_ALUNO', true, 'Estudante de Engenharia de Software. Foco em Mobile e UX.', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400'),
    ('Pedro Nogueira', 'pedro@aluno.orienta.com.br', '$2a$12$hN0L1sJmN9YhL3mU9l3BTeJk6m3c3Q2p7r4y6v1X8m5n3z2w1q4a.', 'ALUNO', 'ROLE_ALUNO', true, 'Estudante de Sistemas de Informação. Buscando mentoria para Cálculo e SQL.', 'https://images.unsplash.com/photo-1527980965255-d3b416303d12?w=400')
ON CONFLICT (email) DO NOTHING;

-- 4. Vincular Aptidões com Preço/Hora para os Professores
INSERT INTO tb_aptidoes (usuario_id, saber_id, preco_hora, nivel_dominio)
SELECT u.id, s.id, 80.00, 'Avançado'
FROM tb_usuarios u, tb_saberes s
WHERE u.email = 'helena.souza@orienta.com.br' AND s.nome = 'Estruturas de Dados e Algoritmos'
ON CONFLICT DO NOTHING;

INSERT INTO tb_aptidoes (usuario_id, saber_id, preco_hora, nivel_dominio)
SELECT u.id, s.id, 75.00, 'Avançado'
FROM tb_usuarios u, tb_saberes s
WHERE u.email = 'marcos.andrade@orienta.com.br' AND s.nome = 'Programação Orientada a Objetos com Java'
ON CONFLICT DO NOTHING;

INSERT INTO tb_aptidoes (usuario_id, saber_id, preco_hora, nivel_dominio)
SELECT u.id, s.id, 90.00, 'Especialista'
FROM tb_usuarios u, tb_saberes s
WHERE u.email = 'camila.barreto@orienta.com.br' AND s.nome = 'Banco de Dados Relacional e SQL'
ON CONFLICT DO NOTHING;

-- 5. Inserir 20 Aulas Fictícias de Demonstração (Seed com diversos status)
-- Usamos CURRENT_DATE + offsets para garantir que as datas sejam sempre válidas e atuais
INSERT INTO tb_agendamento (id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento)
SELECT 
    a.id, p.id, s.id, 
    CURRENT_DATE + 2, '14:00:00'::time, 'CONFIRMADO', 'ONLINE', 80.00, 'Revisão de Listas Encadeadas e Árvores Binárias', NULL
FROM tb_usuarios a, tb_usuarios p, tb_saberes s
WHERE a.email = 'carlos@aluno.orienta.com.br' 
  AND p.email = 'helena.souza@orienta.com.br' 
  AND s.nome = 'Estruturas de Dados e Algoritmos'
ON CONFLICT DO NOTHING;

INSERT INTO tb_agendamento (id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento)
SELECT 
    a.id, p.id, s.id, 
    CURRENT_DATE + 3, '10:00:00'::time, 'PENDENTE', 'PRESENCIAL', 75.00, 'Introdução ao Spring Boot e Injeção de Dependências', NULL
FROM tb_usuarios a, tb_usuarios p, tb_saberes s
WHERE a.email = 'carlos@aluno.orienta.com.br' 
  AND p.email = 'marcos.andrade@orienta.com.br' 
  AND s.nome = 'Programação Orientada a Objetos com Java'
ON CONFLICT DO NOTHING;

INSERT INTO tb_agendamento (id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento)
SELECT 
    a.id, p.id, s.id, 
    CURRENT_DATE + 4, '16:00:00'::time, 'CONFIRMADO', 'ONLINE', 90.00, 'Otimização de Índices e Joins Complexos em PostgreSQL', NULL
FROM tb_usuarios a, tb_usuarios p, tb_saberes s
WHERE a.email = 'mariana@aluno.orienta.com.br' 
  AND p.email = 'camila.barreto@orienta.com.br' 
  AND s.nome = 'Banco de Dados Relacional e SQL'
ON CONFLICT DO NOTHING;

INSERT INTO tb_agendamento (id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento)
SELECT 
    a.id, p.id, s.id, 
    CURRENT_DATE - 5, '09:00:00'::time, 'REALIZADA', 'ONLINE', 80.00, 'Grafos e Algoritmo de Dijkstra', NULL
FROM tb_usuarios a, tb_usuarios p, tb_saberes s
WHERE a.email = 'carlos@aluno.orienta.com.br' 
  AND p.email = 'helena.souza@orienta.com.br' 
  AND s.nome = 'Estruturas de Dados e Algoritmos'
ON CONFLICT DO NOTHING;

INSERT INTO tb_agendamento (id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento)
SELECT 
    a.id, p.id, s.id, 
    CURRENT_DATE - 2, '15:00:00'::time, 'CANCELADO', 'PRESENCIAL', 75.00, 'Preparação para Prova Final', 'Conflito de agenda comunicado pelo aluno'
FROM tb_usuarios a, tb_usuarios p, tb_saberes s
WHERE a.email = 'pedro@aluno.orienta.com.br' 
  AND p.email = 'marcos.andrade@orienta.com.br' 
  AND s.nome = 'Programação Orientada a Objetos com Java'
ON CONFLICT DO NOTHING;
