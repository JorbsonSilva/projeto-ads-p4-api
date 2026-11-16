-- Dados sintéticos locais: este diretório só é incluído pelo perfil Spring "dev".
-- As contas usam e-mails .test e não recebem CPF ou dados bancários reais.
DO $$
BEGIN
    IF (
        SELECT COUNT(*)
        FROM tb_saberes
        WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
    ) < 3 THEN
        RAISE EXCEPTION 'O catálogo precisa conter ao menos 3 disciplinas com conteúdo para o seed dev.';
    END IF;
END
$$;

SELECT setval(
    pg_get_serial_sequence('tb_usuarios', 'id'),
    GREATEST(
        COALESCE((SELECT MAX(id) FROM tb_usuarios), 0),
        COALESCE(pg_sequence_last_value(pg_get_serial_sequence('tb_usuarios', 'id')), 0),
        1
    ),
    TRUE
);

WITH first_names(items) AS (
    VALUES (ARRAY[
        'Ana', 'Bruno', 'Camila', 'Daniel', 'Eduarda', 'Felipe', 'Giovana', 'Henrique',
        'Isabela', 'Joao', 'Karina', 'Lucas', 'Marina', 'Nicolas', 'Olivia', 'Pedro',
        'Rafaela', 'Samuel', 'Talita', 'Victor'
    ]::TEXT[])
),
last_names(items) AS (
    VALUES (ARRAY[
        'Almeida', 'Barbosa', 'Campos', 'Dias', 'Esteves', 'Ferreira', 'Gomes', 'Lima',
        'Mendes', 'Nogueira', 'Oliveira', 'Pereira', 'Queiroz', 'Ramos', 'Santos', 'Teixeira',
        'Urbano', 'Vieira', 'Xavier', 'Yamada', 'Zanetti', 'Azevedo', 'Batista', 'Costa', 'Duarte'
    ]::TEXT[])
),
cities(city, state) AS (
    VALUES
        ('Recife', 'PE'),
        ('Salvador', 'BA'),
        ('Brasilia', 'DF'),
        ('Belo Horizonte', 'MG'),
        ('Porto Alegre', 'RS'),
        ('Sao Paulo', 'SP')
)
INSERT INTO tb_usuarios (
    nome, email, senha, tipo, papel, email_verificado, data_nascimento,
    bio_aluno, bio_professor, cidade, estado, titulacao,
    preco_hora_online, preco_hora_presencial, aceita_online,
    oferece_presencial, regiao_atendimento, raio_atendimento_km,
    nota_media, total_avaliacoes
)
SELECT
    first_names.items[((account.sequence - 1) % 20) + 1] || ' '
        || last_names.items[((account.sequence - 1) / 20) + 1],
    'demo.usuario.' || LPAD(account.sequence::TEXT, 3, '0') || '@orienta.test',
    '$2b$12$KkTzhCxYoPmejJqtYx0X7.RX4qOGU4DW4Dgkcbivyp2tvPpu.YuOi',
    'ALUNO',
    'ROLE_ALUNO',
    TRUE,
    MAKE_DATE(1988 + account.sequence % 20, 1 + account.sequence % 12, 1 + account.sequence % 28)::TEXT,
    'Aluno de demonstracao interessado em aprender novas disciplinas.',
    'Professor de demonstracao; conteudos e topicos estao no catalogo de disciplinas.',
    cities.city,
    cities.state,
    (ARRAY[
        'Graduacao em Tecnologia', 'Licenciatura em Matematica', 'Graduacao em Ciencias',
        'Bacharelado em Administracao', 'Graduacao em Letras'
    ])[((account.sequence - 1) % 5) + 1],
    35 + (account.sequence % 12) * 5,
    45 + (account.sequence % 14) * 5,
    TRUE,
    account.sequence % 4 <> 0,
    CASE WHEN account.sequence % 4 <> 0 THEN cities.city || ' - regiao central' END,
    10,
    NULL,
    0
FROM GENERATE_SERIES(1, 500) AS account(sequence)
CROSS JOIN first_names
CROSS JOIN last_names
JOIN cities ON cities.city = (ARRAY[
    'Recife', 'Salvador', 'Brasilia', 'Belo Horizonte', 'Porto Alegre', 'Sao Paulo'
])[((account.sequence - 1) % 6) + 1]
ON CONFLICT (email) DO NOTHING;

INSERT INTO tb_usuario_papeis (usuario_id, papel)
SELECT usuario.id, roles.papel
FROM tb_usuarios AS usuario
CROSS JOIN (VALUES ('ROLE_ALUNO'), ('ROLE_PROFESSOR')) AS roles(papel)
WHERE usuario.email LIKE 'demo.usuario.%@orienta.test'
ON CONFLICT (usuario_id, papel) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('tb_interesses', 'id'),
    GREATEST(
        COALESCE((SELECT MAX(id) FROM tb_interesses), 0),
        COALESCE(pg_sequence_last_value(pg_get_serial_sequence('tb_interesses', 'id')), 0),
        1
    ),
    TRUE
);

WITH seed_users AS (
    SELECT
        id,
        SUBSTRING(email FROM '^demo\.usuario\.([0-9]{3})@orienta\.test$')::INTEGER AS sequence
    FROM tb_usuarios
    WHERE email LIKE 'demo.usuario.%@orienta.test'
),
subjects AS (
    SELECT
        id,
        (ROW_NUMBER() OVER (ORDER BY id) - 1)::INTEGER AS subject_index,
        (COUNT(*) OVER ())::INTEGER AS subject_count
    FROM tb_saberes
    WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
),
account_subjects AS (
    SELECT users.id AS user_id, subject.id AS saber_id
    FROM seed_users AS users
    CROSS JOIN GENERATE_SERIES(0, 2) AS slot(value)
    JOIN subjects AS subject
      ON subject.subject_index = MOD(users.sequence - 1 + slot.value, subject.subject_count)
)
INSERT INTO tb_interesses (usuario_id, saber_id)
SELECT account_subjects.user_id, account_subjects.saber_id
FROM account_subjects
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_interesses AS existing
    WHERE existing.usuario_id = account_subjects.user_id
      AND existing.saber_id = account_subjects.saber_id
);

SELECT setval(
    pg_get_serial_sequence('tb_aptidoes', 'id'),
    GREATEST(
        COALESCE((SELECT MAX(id) FROM tb_aptidoes), 0),
        COALESCE(pg_sequence_last_value(pg_get_serial_sequence('tb_aptidoes', 'id')), 0),
        1
    ),
    TRUE
);

WITH seed_users AS (
    SELECT
        id,
        SUBSTRING(email FROM '^demo\.usuario\.([0-9]{3})@orienta\.test$')::INTEGER AS sequence
    FROM tb_usuarios
    WHERE email LIKE 'demo.usuario.%@orienta.test'
),
subjects AS (
    SELECT
        id,
        (ROW_NUMBER() OVER (ORDER BY id) - 1)::INTEGER AS subject_index,
        (COUNT(*) OVER ())::INTEGER AS subject_count
    FROM tb_saberes
    WHERE descricao IS NOT NULL AND jsonb_array_length(topicos_populares) > 0
),
account_skills AS (
    SELECT
        users.id AS user_id,
        subject.id AS saber_id,
        (ARRAY['Intermediario', 'Avancado', 'Especialista'])[
            MOD(users.sequence + slot.value, 3) + 1
        ] AS level,
        40 + MOD(users.sequence * 3 + slot.value * 11, 61) AS lesson_price
    FROM seed_users AS users
    CROSS JOIN GENERATE_SERIES(0, 2) AS slot(value)
    JOIN subjects AS subject
      ON subject.subject_index = MOD(users.sequence - 1 + slot.value, subject.subject_count)
)
INSERT INTO tb_aptidoes (usuario_id, saber_id, nivel_dominio, preco_hora)
SELECT user_id, saber_id, level, lesson_price
FROM account_skills
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_aptidoes AS existing
    WHERE existing.usuario_id = account_skills.user_id
      AND existing.saber_id = account_skills.saber_id
);

SELECT setval(
    pg_get_serial_sequence('tb_disponibilidade', 'id'),
    GREATEST(
        COALESCE((SELECT MAX(id) FROM tb_disponibilidade), 0),
        COALESCE(pg_sequence_last_value(pg_get_serial_sequence('tb_disponibilidade', 'id')), 0),
        1
    ),
    TRUE
);

WITH seed_users AS (
    SELECT
        id,
        SUBSTRING(email FROM '^demo\.usuario\.([0-9]{3})@orienta\.test$')::INTEGER AS sequence
    FROM tb_usuarios
    WHERE email LIKE 'demo.usuario.%@orienta.test'
),
days(day_index, day_name) AS (
    VALUES
        (0, 'SEGUNDA'),
        (1, 'TERCA'),
        (2, 'QUARTA'),
        (3, 'QUINTA'),
        (4, 'SEXTA'),
        (5, 'SABADO'),
        (6, 'DOMINGO')
),
account_availability AS (
    SELECT
        users.id AS user_id,
        days.day_name,
        8 + MOD(users.sequence + slot.value * 4, 9) AS start_hour
    FROM seed_users AS users
    CROSS JOIN GENERATE_SERIES(0, 1) AS slot(value)
    JOIN days
      ON days.day_index = MOD(users.sequence - 1 + slot.value * 2, 7)
)
INSERT INTO tb_disponibilidade (usuario_id, dia_semana, hora_inicio, hora_fim)
SELECT
    user_id,
    day_name,
    MAKE_TIME(start_hour, 0, 0),
    MAKE_TIME(start_hour + 2, 0, 0)
FROM account_availability
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_disponibilidade AS existing
    WHERE existing.usuario_id = account_availability.user_id
      AND existing.dia_semana = account_availability.day_name
      AND existing.hora_inicio = MAKE_TIME(account_availability.start_hour, 0, 0)
      AND existing.hora_fim = MAKE_TIME(account_availability.start_hour + 2, 0, 0)
);

DO $$
BEGIN
    IF (
        SELECT COUNT(*)
        FROM tb_usuarios
        WHERE email LIKE 'demo.usuario.%@orienta.test'
    ) <> 500 THEN
        RAISE EXCEPTION 'O seed dev deveria deixar exatamente 500 contas sinteticas.';
    END IF;
END
$$;
