-- Os professores legados do seed V4 também são usados na demonstração local.
-- Suas disponibilidades ficam no banco, como as das 500 contas dev.
WITH availability(email, day_name, start_time, end_time) AS (
    VALUES
        ('helena.souza@orienta.com.br', 'SEGUNDA', '08:00:00'::TIME, '18:00:00'::TIME),
        ('helena.souza@orienta.com.br', 'QUARTA', '14:00:00'::TIME, '20:00:00'::TIME),
        ('marcos.andrade@orienta.com.br', 'TERCA', '09:00:00'::TIME, '17:00:00'::TIME),
        ('marcos.andrade@orienta.com.br', 'QUINTA', '09:00:00'::TIME, '17:00:00'::TIME),
        ('camila.barreto@orienta.com.br', 'SEGUNDA', '08:00:00'::TIME, '16:00:00'::TIME),
        ('camila.barreto@orienta.com.br', 'QUARTA', '12:00:00'::TIME, '20:00:00'::TIME),
        ('rafael.duarte@orienta.com.br', 'TERCA', '08:00:00'::TIME, '16:00:00'::TIME),
        ('rafael.duarte@orienta.com.br', 'SEXTA', '10:00:00'::TIME, '18:00:00'::TIME),
        ('fernanda.lima@orienta.com.br', 'SEGUNDA', '09:00:00'::TIME, '17:00:00'::TIME),
        ('fernanda.lima@orienta.com.br', 'QUINTA', '10:00:00'::TIME, '18:00:00'::TIME),
        ('thiago.meireles@orienta.com.br', 'TERCA', '10:00:00'::TIME, '18:00:00'::TIME),
        ('thiago.meireles@orienta.com.br', 'SABADO', '08:00:00'::TIME, '14:00:00'::TIME),
        ('juliana.rocha@orienta.com.br', 'QUARTA', '08:00:00'::TIME, '16:00:00'::TIME),
        ('juliana.rocha@orienta.com.br', 'SEXTA', '12:00:00'::TIME, '20:00:00'::TIME),
        ('lucas.silveira@orienta.com.br', 'TERCA', '08:00:00'::TIME, '16:00:00'::TIME),
        ('lucas.silveira@orienta.com.br', 'QUINTA', '12:00:00'::TIME, '20:00:00'::TIME),
        ('beatriz.monteiro@orienta.com.br', 'SEGUNDA', '10:00:00'::TIME, '18:00:00'::TIME),
        ('beatriz.monteiro@orienta.com.br', 'SABADO', '09:00:00'::TIME, '15:00:00'::TIME),
        ('diego.vasconcelos@orienta.com.br', 'QUARTA', '09:00:00'::TIME, '17:00:00'::TIME),
        ('diego.vasconcelos@orienta.com.br', 'SEXTA', '08:00:00'::TIME, '16:00:00'::TIME)
)
INSERT INTO tb_disponibilidade (usuario_id, dia_semana, hora_inicio, hora_fim)
SELECT usuario.id, availability.day_name, availability.start_time, availability.end_time
FROM availability
JOIN tb_usuarios usuario ON usuario.email = availability.email
WHERE NOT EXISTS (
    SELECT 1
    FROM tb_disponibilidade existing
    WHERE existing.usuario_id = usuario.id
      AND existing.dia_semana = availability.day_name
      AND existing.hora_inicio = availability.start_time
      AND existing.hora_fim = availability.end_time
);
