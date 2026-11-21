-- Avaliações demonstrativas para o perfil de Rafael Martins no ambiente dev.
WITH professor_demo AS (
    SELECT id
    FROM tb_usuarios
    WHERE email = 'demo.professor@orienta.test'
),
aulas_concluidas AS (
    SELECT
        agendamento.id AS agendamento_id,
        agendamento.id_aluno AS avaliador_id,
        ROW_NUMBER() OVER (ORDER BY agendamento.data, agendamento.id) AS ordem
    FROM tb_agendamento agendamento
    JOIN professor_demo professor ON professor.id = agendamento.id_professor
    WHERE agendamento.status = 'REALIZADA'
),
comentarios(ordem, comentario) AS (
    VALUES
        (1, 'A explicação foi clara e organizada; saí da aula entendendo melhor o conteúdo.'),
        (2, 'Professor paciente, respondeu minhas dúvidas e trouxe bons exemplos práticos.'),
        (3, 'A aula foi objetiva e muito bem preparada. Recomendo!'),
        (4, 'Consegui revisar os principais pontos e praticar com exercícios.'),
        (5, 'Excelente domínio do conteúdo e ótima didática durante a aula.'),
        (6, 'Ajudou a destravar o assunto com explicações simples e exemplos.')
)
INSERT INTO tb_avaliacoes (
    agendamento_id, avaliador_id, avaliado_id,
    nota, didatica, pontualidade, dominio, comentario, criada_em
)
SELECT
    aula.agendamento_id,
    aula.avaliador_id,
    professor.id,
    5, 5, 5, 5,
    comentarios.comentario,
    agendamento.data + agendamento.hora + INTERVAL '2 hours'
FROM aulas_concluidas aula
JOIN comentarios ON comentarios.ordem = aula.ordem
JOIN tb_agendamento agendamento ON agendamento.id = aula.agendamento_id
CROSS JOIN professor_demo professor
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_avaliacoes existente
    WHERE existente.agendamento_id = aula.agendamento_id
      AND existente.avaliador_id = aula.avaliador_id
);

UPDATE tb_usuarios professor
SET
    nota_media = resumo.media,
    total_avaliacoes = resumo.total
FROM (
    SELECT
        avaliado_id,
        ROUND(AVG(nota)::NUMERIC, 1)::DOUBLE PRECISION AS media,
        COUNT(*)::INTEGER AS total
    FROM tb_avaliacoes
    WHERE avaliado_id = (
        SELECT id FROM tb_usuarios WHERE email = 'demo.professor@orienta.test'
    )
    GROUP BY avaliado_id
) resumo
WHERE professor.id = resumo.avaliado_id;
