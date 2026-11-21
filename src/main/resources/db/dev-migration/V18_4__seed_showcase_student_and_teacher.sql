-- Contas e histórico sintéticos para demonstrações locais.
-- Esta migração fica no diretório exclusivo do perfil Spring "dev".
INSERT INTO tb_usuarios (
    nome, email, senha, tipo, papel, email_verificado, data_nascimento,
    bio_aluno, bio_professor, cidade, estado, titulacao,
    preco_hora_online, preco_hora_presencial, aceita_online,
    oferece_presencial, regiao_atendimento, latitude_aproximada,
    longitude_aproximada, raio_atendimento_km, foto_url, nota_media, total_avaliacoes
)
VALUES
    (
        'Marina Costa', 'demo.aluno@orienta.test',
        '$2b$12$KkTzhCxYoPmejJqtYx0X7.RX4qOGU4DW4Dgkcbivyp2tvPpu.YuOi',
        'ALUNO', 'ROLE_ALUNO', TRUE, '2002-06-15',
        'Estudante de demonstração de Análise e Desenvolvimento de Sistemas.',
        NULL, 'Recife', 'PE', NULL, NULL, NULL, TRUE, FALSE, NULL,
        NULL, NULL, 10,
        'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400',
        NULL, 0
    ),
    (
        'Rafael Martins', 'demo.professor@orienta.test',
        '$2b$12$KkTzhCxYoPmejJqtYx0X7.RX4qOGU4DW4Dgkcbivyp2tvPpu.YuOi',
        'PROFESSOR', 'ROLE_PROFESSOR', TRUE, '1987-03-22',
        NULL, 'Professor de demonstração com aulas e mentorias em tecnologia e matemática.',
        'Recife', 'PE', 'Mestre em Ciência da Computação',
        80.00, 90.00, TRUE, TRUE, 'Boa Viagem',
        -8.1189, -34.8988, 15,
        'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400',
        4.9, 18
    )
ON CONFLICT (email) DO UPDATE SET
    nome = EXCLUDED.nome,
    senha = EXCLUDED.senha,
    tipo = EXCLUDED.tipo,
    papel = EXCLUDED.papel,
    email_verificado = EXCLUDED.email_verificado,
    data_nascimento = EXCLUDED.data_nascimento,
    bio_aluno = EXCLUDED.bio_aluno,
    bio_professor = EXCLUDED.bio_professor,
    cidade = EXCLUDED.cidade,
    estado = EXCLUDED.estado,
    titulacao = EXCLUDED.titulacao,
    preco_hora_online = EXCLUDED.preco_hora_online,
    preco_hora_presencial = EXCLUDED.preco_hora_presencial,
    aceita_online = EXCLUDED.aceita_online,
    oferece_presencial = EXCLUDED.oferece_presencial,
    regiao_atendimento = EXCLUDED.regiao_atendimento,
    latitude_aproximada = EXCLUDED.latitude_aproximada,
    longitude_aproximada = EXCLUDED.longitude_aproximada,
    raio_atendimento_km = EXCLUDED.raio_atendimento_km,
    foto_url = EXCLUDED.foto_url,
    nota_media = EXCLUDED.nota_media,
    total_avaliacoes = EXCLUDED.total_avaliacoes;

INSERT INTO tb_usuario_papeis (usuario_id, papel)
SELECT id, papel
FROM tb_usuarios
WHERE email = 'demo.aluno@orienta.test'
ON CONFLICT (usuario_id, papel) DO NOTHING;

INSERT INTO tb_usuario_papeis (usuario_id, papel)
SELECT id, papel
FROM tb_usuarios
WHERE email = 'demo.professor@orienta.test'
ON CONFLICT (usuario_id, papel) DO NOTHING;

WITH demo_professor AS (
    SELECT id FROM tb_usuarios WHERE email = 'demo.professor@orienta.test'
),
saberes AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS ordem
    FROM tb_saberes
    WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
)
INSERT INTO tb_aptidoes (usuario_id, saber_id, nivel_dominio, preco_hora)
SELECT demo_professor.id, saberes.id, 'Avancado', 80.00
FROM demo_professor
CROSS JOIN saberes
WHERE saberes.ordem <= 3
  AND NOT EXISTS (
      SELECT 1
      FROM tb_aptidoes existente
      WHERE existente.usuario_id = demo_professor.id
        AND existente.saber_id = saberes.id
  );

WITH demo_aluno AS (
    SELECT id FROM tb_usuarios WHERE email = 'demo.aluno@orienta.test'
),
saberes AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS ordem
    FROM tb_saberes
    WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
)
INSERT INTO tb_interesses (usuario_id, saber_id)
SELECT demo_aluno.id, saberes.id
FROM demo_aluno
CROSS JOIN saberes
WHERE saberes.ordem <= 3
  AND NOT EXISTS (
      SELECT 1
      FROM tb_interesses existente
      WHERE existente.usuario_id = demo_aluno.id
        AND existente.saber_id = saberes.id
  );

INSERT INTO tb_disponibilidade (usuario_id, dia_semana, hora_inicio, hora_fim)
SELECT usuario.id, disponibilidade.dia_semana, disponibilidade.hora_inicio, disponibilidade.hora_fim
FROM tb_usuarios usuario
CROSS JOIN (
    VALUES
        ('SEGUNDA', TIME '09:00', TIME '17:00'),
        ('QUARTA', TIME '09:00', TIME '17:00'),
        ('SEXTA', TIME '10:00', TIME '16:00')
) AS disponibilidade(dia_semana, hora_inicio, hora_fim)
WHERE usuario.email = 'demo.professor@orienta.test'
  AND NOT EXISTS (
      SELECT 1
      FROM tb_disponibilidade existente
      WHERE existente.usuario_id = usuario.id
        AND existente.dia_semana = disponibilidade.dia_semana
        AND existente.hora_inicio = disponibilidade.hora_inicio
        AND existente.hora_fim = disponibilidade.hora_fim
  );

CREATE TEMP TABLE demo_showcase_agendamentos ON COMMIT DROP AS
WITH cenarios(
    chave, aluno_email, professor_email, saber_ordem, dias, hora, status,
    modalidade, valor, assunto, justificativa, remarcada
) AS (
    VALUES
        ('aluno-concluida-1', 'demo.aluno@orienta.test', 'demo.professor@orienta.test', 1, -90, TIME '10:00', 'REALIZADA', 'ONLINE', 80.00, 'Estruturas de dados e listas encadeadas', NULL, FALSE),
        ('aluno-concluida-2', 'demo.aluno@orienta.test', 'demo.professor@orienta.test', 2, -35, TIME '14:00', 'REALIZADA', 'ONLINE', 80.00, 'Programação orientada a objetos', NULL, FALSE),
        ('aluno-confirmada', 'demo.aluno@orienta.test', 'demo.professor@orienta.test', 3, 4, TIME '11:00', 'CONFIRMADO', 'PRESENCIAL', 90.00, 'Revisão para avaliação de matemática', NULL, FALSE),
        ('aluno-pendente', 'demo.aluno@orienta.test', 'demo.professor@orienta.test', 1, 11, TIME '15:00', 'PENDENTE', 'ONLINE', 80.00, 'Introdução a algoritmos', NULL, FALSE),
        ('aluno-remarcada', 'demo.aluno@orienta.test', 'demo.professor@orienta.test', 2, 18, TIME '16:00', 'CONFIRMADO', 'ONLINE', 80.00, 'Revisão de programação remarcada', NULL, TRUE),
        ('aluno-outro-prof-concluida', 'demo.aluno@orienta.test', 'demo.usuario.021@orienta.test', 3, -60, TIME '09:00', 'REALIZADA', 'ONLINE', 70.00, 'Fundamentos de cálculo', NULL, FALSE),
        ('aluno-outro-prof-recusada', 'demo.aluno@orienta.test', 'demo.usuario.022@orienta.test', 1, -9, TIME '13:00', 'RECUSADO', 'PRESENCIAL', 65.00, 'Aula de revisão presencial', 'Indisponibilidade de horário.', FALSE),
        ('aluno-outro-prof-pendente', 'demo.aluno@orienta.test', 'demo.usuario.023@orienta.test', 2, 20, TIME '10:00', 'PENDENTE', 'ONLINE', 75.00, 'Mentoria de desenvolvimento mobile', NULL, FALSE),
        ('prof-aluno-001-concluida', 'demo.usuario.001@orienta.test', 'demo.professor@orienta.test', 1, -120, TIME '09:00', 'REALIZADA', 'ONLINE', 80.00, 'Acompanhamento de algoritmos', NULL, FALSE),
        ('prof-aluno-002-concluida', 'demo.usuario.002@orienta.test', 'demo.professor@orienta.test', 2, -50, TIME '11:00', 'REALIZADA', 'PRESENCIAL', 90.00, 'Prática de programação Java', NULL, FALSE),
        ('prof-aluno-003-concluida', 'demo.usuario.003@orienta.test', 'demo.professor@orienta.test', 3, -12, TIME '15:00', 'REALIZADA', 'ONLINE', 80.00, 'Exercícios de matemática aplicada', NULL, FALSE),
        ('prof-aluno-004-confirmada', 'demo.usuario.004@orienta.test', 'demo.professor@orienta.test', 1, 5, TIME '09:00', 'CONFIRMADO', 'ONLINE', 80.00, 'Mentoria de estruturas de dados', NULL, FALSE),
        ('prof-aluno-005-confirmada', 'demo.usuario.005@orienta.test', 'demo.professor@orienta.test', 2, 9, TIME '13:00', 'CONFIRMADO', 'PRESENCIAL', 90.00, 'Aula de programação Java', NULL, FALSE),
        ('prof-aluno-006-pendente', 'demo.usuario.006@orienta.test', 'demo.professor@orienta.test', 3, 14, TIME '10:00', 'PENDENTE', 'ONLINE', 80.00, 'Dúvidas de matemática', NULL, FALSE),
        ('prof-aluno-007-pendente', 'demo.usuario.007@orienta.test', 'demo.professor@orienta.test', 1, 17, TIME '14:00', 'PENDENTE', 'ONLINE', 80.00, 'Preparação para prova de algoritmos', NULL, FALSE),
        ('prof-aluno-008-recusada', 'demo.usuario.008@orienta.test', 'demo.professor@orienta.test', 2, -15, TIME '16:00', 'RECUSADO', 'PRESENCIAL', 90.00, 'Reforço presencial', 'Conflito de horário na agenda.', FALSE),
        ('prof-aluno-009-recusada', 'demo.usuario.009@orienta.test', 'demo.professor@orienta.test', 3, -8, TIME '08:00', 'RECUSADO', 'ONLINE', 80.00, 'Revisão de cálculo', 'Não há disponibilidade nesse horário.', FALSE),
        ('prof-aluno-010-remarcada', 'demo.usuario.010@orienta.test', 'demo.professor@orienta.test', 1, 23, TIME '15:00', 'CONFIRMADO', 'ONLINE', 80.00, 'Mentoria de algoritmos remarcada', NULL, TRUE),
        ('prof-aluno-011-concluida', 'demo.usuario.011@orienta.test', 'demo.professor@orienta.test', 2, -3, TIME '12:00', 'REALIZADA', 'ONLINE', 90.00, 'Revisão de orientação a objetos', NULL, FALSE)
),
alunos AS (
    SELECT id, email FROM tb_usuarios
)
SELECT
    cenarios.chave,
    aluno.id AS aluno_id,
    professor.id AS professor_id,
    saber.id AS saber_id,
    CURRENT_DATE + cenarios.dias AS data,
    cenarios.hora,
    cenarios.status,
    cenarios.modalidade,
    cenarios.valor::NUMERIC(10, 2) AS valor,
    cenarios.assunto,
    cenarios.justificativa,
    cenarios.remarcada,
    NULL::BIGINT AS agendamento_id
FROM cenarios
JOIN alunos aluno ON aluno.email = cenarios.aluno_email
JOIN tb_usuarios professor ON professor.email = cenarios.professor_email
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS ordem
    FROM tb_saberes
    WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
) saber ON saber.ordem = cenarios.saber_ordem;

INSERT INTO tb_agendamento (
    id_aluno, id_professor, saber_id, data, hora, status, modalidade,
    valor_aplicado, assunto, justificativa_cancelamento, criado_em
)
SELECT
    seed.aluno_id,
    seed.professor_id,
    seed.saber_id,
    seed.data,
    seed.hora,
    seed.status,
    seed.modalidade,
    seed.valor,
    seed.assunto,
    seed.justificativa,
    seed.data + seed.hora
FROM demo_showcase_agendamentos seed
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_agendamento existente
    WHERE existente.id_aluno = seed.aluno_id
      AND existente.id_professor = seed.professor_id
      AND existente.data = seed.data
      AND existente.hora = seed.hora
);

UPDATE demo_showcase_agendamentos seed
SET agendamento_id = agendamento.id
FROM tb_agendamento agendamento
WHERE agendamento.id_aluno = seed.aluno_id
  AND agendamento.id_professor = seed.professor_id
  AND agendamento.data = seed.data
  AND agendamento.hora = seed.hora;

WITH transicoes AS (
    SELECT
        seed.agendamento_id,
        NULL::VARCHAR(30) AS status_anterior,
        'PENDENTE'::VARCHAR(30) AS status_novo,
        'Solicitação inicial realizada pelo aluno.' AS motivo,
        seed.aluno_id AS alterado_por_id,
        seed.data + seed.hora - INTERVAL '3 days' AS data_hora
    FROM demo_showcase_agendamentos seed
    UNION ALL
    SELECT
        seed.agendamento_id, 'PENDENTE', 'CONFIRMADO',
        'Aula aceita e confirmada pelo professor.',
        seed.professor_id, seed.data + seed.hora - INTERVAL '2 days'
    FROM demo_showcase_agendamentos seed
    WHERE seed.status IN ('CONFIRMADO', 'REALIZADA')
    UNION ALL
    SELECT
        seed.agendamento_id, 'CONFIRMADO', 'EM_ANDAMENTO',
        'Aula iniciada pelo professor.',
        seed.professor_id, seed.data + seed.hora
    FROM demo_showcase_agendamentos seed
    WHERE seed.status = 'REALIZADA'
    UNION ALL
    SELECT
        seed.agendamento_id, 'EM_ANDAMENTO', 'AGUARDANDO_CONFIRMACAO',
        'Aula encerrada; aguardando confirmação.',
        seed.aluno_id, seed.data + seed.hora + INTERVAL '1 hour'
    FROM demo_showcase_agendamentos seed
    WHERE seed.status = 'REALIZADA'
    UNION ALL
    SELECT
        seed.agendamento_id, 'AGUARDANDO_CONFIRMACAO', 'REALIZADA',
        'Aula concluída e confirmada pelas partes.',
        seed.professor_id, seed.data + seed.hora + INTERVAL '2 hours'
    FROM demo_showcase_agendamentos seed
    WHERE seed.status = 'REALIZADA'
    UNION ALL
    SELECT
        seed.agendamento_id, 'PENDENTE', 'RECUSADO',
        COALESCE(seed.justificativa, 'Solicitação recusada pelo professor.'),
        seed.professor_id, seed.data + seed.hora - INTERVAL '2 days'
    FROM demo_showcase_agendamentos seed
    WHERE seed.status = 'RECUSADO'
    UNION ALL
    SELECT
        seed.agendamento_id, 'PENDENTE', 'CANCELADO',
        COALESCE(seed.justificativa, 'Agendamento cancelado para demonstração.'),
        seed.aluno_id, seed.data + seed.hora - INTERVAL '2 days'
    FROM demo_showcase_agendamentos seed
    WHERE seed.status = 'CANCELADO'
    UNION ALL
    SELECT
        seed.agendamento_id, 'CONFIRMADO', 'CONFIRMADO',
        'Aula remarcada a pedido do aluno; novo horário confirmado.',
        seed.aluno_id, seed.data + seed.hora - INTERVAL '1 day'
    FROM demo_showcase_agendamentos seed
    WHERE seed.remarcada
)
INSERT INTO tb_agendamento_historico (
    agendamento_id, status_anterior, status_novo, motivo, alterado_por_id, data_hora
)
SELECT
    transicoes.agendamento_id,
    transicoes.status_anterior,
    transicoes.status_novo,
    transicoes.motivo,
    transicoes.alterado_por_id,
    transicoes.data_hora
FROM transicoes
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_agendamento_historico existente
    WHERE existente.agendamento_id = transicoes.agendamento_id
      AND existente.status_anterior IS NOT DISTINCT FROM transicoes.status_anterior
      AND existente.status_novo = transicoes.status_novo
      AND existente.motivo = transicoes.motivo
);

INSERT INTO tb_conversas (agendamento_id, ativa, criada_em, atualizada_em)
SELECT seed.agendamento_id, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM demo_showcase_agendamentos seed
WHERE seed.status NOT IN ('RECUSADO', 'CANCELADO')
  AND NOT EXISTS (
      SELECT 1 FROM tb_conversas existente
      WHERE existente.agendamento_id = seed.agendamento_id
  );

WITH mensagens(numero, conteudo) AS (
    VALUES
        (1, 'Olá! Quero me preparar para nossa aula sobre o tema combinado.'),
        (2, 'Oi! Vamos revisar os conceitos e praticar alguns exercícios juntos.'),
        (3, 'Perfeito, vou separar minhas dúvidas e anotações.'),
        (4, 'Combinado! Até a aula. Se precisar, pode me chamar por aqui.')
)
INSERT INTO tb_mensagens (
    conversa_id, remetente_id, conteudo, tipo, lida, lida_em, enviada_em
)
SELECT
    conversa.id,
    CASE WHEN mensagens.numero % 2 = 1 THEN seed.aluno_id ELSE seed.professor_id END,
    mensagens.conteudo,
    'TEXTO',
    mensagens.numero < 4,
    CASE WHEN mensagens.numero < 4 THEN horario.enviada_em + INTERVAL '1 minute' END,
    horario.enviada_em
FROM demo_showcase_agendamentos seed
JOIN tb_conversas conversa ON conversa.agendamento_id = seed.agendamento_id
CROSS JOIN mensagens
CROSS JOIN LATERAL (
    SELECT LEAST(
        seed.data + seed.hora - INTERVAL '2 days'
            + (mensagens.numero - 1) * INTERVAL '10 minutes',
        CURRENT_TIMESTAMP - INTERVAL '1 minute'
    ) AS enviada_em
) horario
WHERE seed.status NOT IN ('RECUSADO', 'CANCELADO')
  AND NOT EXISTS (
      SELECT 1
      FROM tb_mensagens existente
      WHERE existente.conversa_id = conversa.id
        AND existente.remetente_id = CASE
            WHEN mensagens.numero % 2 = 1 THEN seed.aluno_id
            ELSE seed.professor_id
        END
        AND existente.conteudo = mensagens.conteudo
  );

UPDATE tb_conversas conversa
SET atualizada_em = COALESCE(
    (SELECT MAX(mensagem.enviada_em) FROM tb_mensagens mensagem WHERE mensagem.conversa_id = conversa.id),
    conversa.atualizada_em
)
WHERE conversa.agendamento_id IN (
    SELECT agendamento_id FROM demo_showcase_agendamentos
);
