CREATE TABLE IF NOT EXISTS tb_usuario_papeis (
    usuario_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    papel VARCHAR(30) NOT NULL
        CHECK (papel IN ('ROLE_ALUNO', 'ROLE_PROFESSOR', 'ROLE_ADMIN')),
    PRIMARY KEY (usuario_id, papel)
);

INSERT INTO tb_usuario_papeis (usuario_id, papel)
SELECT id, COALESCE(papel, 'ROLE_ALUNO')
FROM tb_usuarios
ON CONFLICT (usuario_id, papel) DO NOTHING;
