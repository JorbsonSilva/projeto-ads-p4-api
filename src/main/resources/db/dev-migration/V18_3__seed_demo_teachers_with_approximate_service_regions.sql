-- Localiza as contas de demonstração em regiões selecionáveis no app.
-- Os pontos representam centroides aproximados de bairros; nenhum endereço residencial é armazenado.
WITH regioes(ordem, cidade, estado, regiao, latitude, longitude) AS (
    VALUES
        (1, 'Caruaru', 'PE', 'Centro', -8.2839, -35.9761),
        (2, 'Caruaru', 'PE', 'Maurício de Nassau', -8.2750, -35.9730),
        (3, 'Caruaru', 'PE', 'Indianópolis', -8.3030, -35.9670),
        (4, 'Caruaru', 'PE', 'Boa Vista', -8.2900, -35.9630),
        (5, 'Caruaru', 'PE', 'Salgado', -8.3000, -35.9800),
        (6, 'Recife', 'PE', 'Boa Viagem', -8.1189, -34.8988),
        (7, 'Recife', 'PE', 'Casa Forte', -8.0267, -34.9197),
        (8, 'Recife', 'PE', 'Graças', -8.0416, -34.8974),
        (9, 'Recife', 'PE', 'Madalena', -8.0560, -34.9110),
        (10, 'Recife', 'PE', 'Várzea', -8.0539, -34.9511),
        (11, 'Campina Grande', 'PB', 'Centro', -7.2307, -35.8817),
        (12, 'Campina Grande', 'PB', 'Catolé', -7.2390, -35.8760),
        (13, 'Campina Grande', 'PB', 'Bodocongó', -7.2180, -35.9100),
        (14, 'Campina Grande', 'PB', 'Liberdade', -7.2390, -35.8910),
        (15, 'Campina Grande', 'PB', 'Prata', -7.2240, -35.8900),
        (16, 'João Pessoa', 'PB', 'Centro', -7.1195, -34.8450),
        (17, 'João Pessoa', 'PB', 'Manaíra', -7.1040, -34.8340),
        (18, 'João Pessoa', 'PB', 'Bessa', -7.0740, -34.8340),
        (19, 'João Pessoa', 'PB', 'Tambaú', -7.1160, -34.8240),
        (20, 'João Pessoa', 'PB', 'Bancários', -7.1550, -34.8300)
),
contas AS (
    SELECT
        usuario.id,
        SUBSTRING(usuario.email FROM '^demo\.usuario\.([0-9]{3})@orienta\.test$')::INTEGER AS sequencia
    FROM tb_usuarios usuario
    WHERE usuario.email LIKE 'demo.usuario.%@orienta.test'
)
UPDATE tb_usuarios usuario
SET
    cidade = regioes.cidade,
    estado = regioes.estado,
    oferece_presencial = contas.sequencia % 4 <> 0,
    regiao_atendimento = CASE
        WHEN contas.sequencia % 4 <> 0 THEN regioes.regiao
        ELSE NULL
    END,
    latitude_aproximada = CASE
        WHEN contas.sequencia % 4 <> 0 THEN regioes.latitude
        ELSE NULL
    END,
    longitude_aproximada = CASE
        WHEN contas.sequencia % 4 <> 0 THEN regioes.longitude
        ELSE NULL
    END,
    raio_atendimento_km = CASE
        WHEN contas.sequencia % 4 <> 0 THEN 10
        ELSE NULL
    END
FROM contas
JOIN regioes ON regioes.ordem = ((contas.sequencia - 1) % 20) + 1
WHERE usuario.id = contas.id;
