-- ==============================================================================
-- SEED OFICIAL DA BANCA EXAMINADORA — ORIENTA 2.0 (FASE 7 / TAREFA [BACK-020])
-- Carga determinística para homologação end-to-end e roteiro de apresentação
-- Senha padrão para todas as contas: Senha@123 (BCrypt hash)
-- Hash BCrypt: $2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO
-- ==============================================================================

-- 0. Garantir existência de tabelas financeiras caso executado em ordem Flyway V10
CREATE TABLE IF NOT EXISTS tb_saque (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE RESTRICT,
    valor DECIMAL(10,2) NOT NULL,
    taxa_aplicada DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    valor_liquido DECIMAL(10,2) NOT NULL,
    status_saque VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    tipo_chave_pix VARCHAR(30),
    chave_pix VARCHAR(255),
    dados_bancarios TEXT,
    protocolo_saque VARCHAR(50) NOT NULL UNIQUE,
    data_solicitacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_processamento TIMESTAMP,
    observacoes TEXT
);

CREATE TABLE IF NOT EXISTS tb_transacao_financeira (
    id BIGSERIAL PRIMARY KEY,
    professor_id BIGINT NOT NULL REFERENCES tb_usuarios(id) ON DELETE RESTRICT,
    agendamento_id BIGINT,
    saque_id BIGINT,
    tipo_transacao VARCHAR(30) NOT NULL,
    valor_bruto DECIMAL(10,2) NOT NULL,
    taxa_plataforma DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    valor_liquido DECIMAL(10,2) NOT NULL,
    descricao VARCHAR(255),
    data_transacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Garantir colunas expandidas
ALTER TABLE tb_tickets_suporte ADD COLUMN IF NOT EXISTS assunto VARCHAR(150);
ALTER TABLE tb_tickets_suporte ADD COLUMN IF NOT EXISTS resposta_admin TEXT;
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS protocolo VARCHAR(50);
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS parecer_moderador TEXT;
ALTER TABLE tb_denuncias ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;

-- 1. Inserção / Atualização dos Usuários Oficiais de Banca
INSERT INTO tb_usuarios (id, nome, email, senha, tipo, papel, cpf, email_verificado, bio_aluno, bio_professor, foto_url, nota_media, total_avaliacoes, oferece_presencial, regiao_atendimento, latitude_aproximada, longitude_aproximada, raio_atendimento_km)
VALUES
(1001, 'Gabriel Aluno (Banca)', 'aluno.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ALUNO', 'ROLE_ALUNO', '111.222.333-44', true, 'Estudante de Engenharia de Software no 4º período. Foco em Banco de Dados, Algoritmos e Mobile.', NULL, 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL),
(1002, 'Prof. Dr. Carlos Eduardo', 'prof.carlos@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '222.333.444-55', true, NULL, 'Doutor em Ciência da Computação (UFSCar). Mais de 10 anos lecionando Bancos de Dados Relacionais e Arquitetura de Software.', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400', 4.9, 15, true, 'Centro / Bela Vista', -23.550520, -46.633308, 10),
(1003, 'Profa. Ma. Juliana Oliveira', 'profa.juliana@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '333.444.555-66', true, NULL, 'Mestre em Matemática Pura (USP). Especialista no desbloqueio pedagógico em Cálculo I, II e Álgebra Linear.', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400', 4.8, 12, true, 'Zona Sul / Moema', -23.600000, -46.660000, 15),
(1004, 'Mariana Coordenadora (Admin)', 'admin.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ADMIN', 'ROLE_ADMIN', '444.555.666-77', true, NULL, NULL, 'https://images.unsplash.com/photo-1580894732454-defa10bcfdbd?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO UPDATE SET
    nome = EXCLUDED.nome,
    email = EXCLUDED.email,
    papel = EXCLUDED.papel,
    tipo = EXCLUDED.tipo,
    oferece_presencial = EXCLUDED.oferece_presencial,
    regiao_atendimento = EXCLUDED.regiao_atendimento,
    latitude_aproximada = EXCLUDED.latitude_aproximada,
    longitude_aproximada = EXCLUDED.longitude_aproximada,
    raio_atendimento_km = EXCLUDED.raio_atendimento_km;

-- 2. Disciplinas do Catálogo Acadêmico (tb_saberes)
INSERT INTO tb_saberes (id, nome, categoria) VALUES
(201, 'Cálculo Diferencial e Integral I (Banca)', 'Matemática e Exatas'),
(202, 'Banco de Dados Relacional e SQL (Banca)', 'Engenharia de Software'),
(203, 'Estruturas de Dados e Algoritmos (Banca)', 'Ciência da Computação')
ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome, categoria = EXCLUDED.categoria;

-- 3. Aptidões dos Professores com Preço-Hora
INSERT INTO tb_aptidoes (id, usuario_id, saber_id, preco_hora, nivel_dominio) VALUES
(501, 1002, 202, 70.00, 'Especialista'),
(502, 1002, 203, 75.00, 'Avançado'),
(503, 1003, 201, 65.00, 'Mestrado')
ON CONFLICT (id) DO NOTHING;

-- 4. Disponibilidades Semanais dos Professores
INSERT INTO tb_disponibilidade (id, usuario_id, dia_semana, hora_inicio, hora_fim) VALUES
(601, 1002, 'SEGUNDA', '08:00:00', '18:00:00'),
(602, 1002, 'QUARTA', '14:00:00', '20:00:00'),
(603, 1003, 'TERCA', '09:00:00', '17:00:00'),
(604, 1003, 'QUINTA', '10:00:00', '19:00:00')
ON CONFLICT (id) DO NOTHING;

-- 5. Histórico Completo de Agendamentos (REALIZADA, CONFIRMADO, PENDENTE, CANCELADO)
INSERT INTO tb_agendamento (id, id_aluno, id_professor, saber_id, data, hora, status, modalidade, valor_aplicado, assunto, justificativa_cancelamento, regiao_ou_local_referencia) VALUES
-- Aula 1: REALIZADA com Sessão Concluída e Avaliação 5 Estrelas
(1001, 1001, 1002, 202, CURRENT_DATE - 3, '14:00:00'::time, 'REALIZADA', 'ONLINE', 70.00, 'Normalização de Dados e Índices B-Tree', NULL, 'Online via Orienta Meet'),

-- Aula 2: CONFIRMADA para amanhã (+ 2 dias, respeitando antecedência > 24h)
(1002, 1001, 1002, 203, CURRENT_DATE + 2, '15:00:00'::time, 'CONFIRMADO', 'PRESENCIAL', 75.00, 'Árvores Binárias de Busca e Grafos', NULL, 'Centro / Bela Vista'),

-- Aula 3: PENDENTE aguardando aceite docente (+ 3 dias)
(1003, 1001, 1003, 201, CURRENT_DATE + 3, '10:00:00'::time, 'PENDENTE', 'ONLINE', 65.00, 'Revisão de Derivadas e Regra da Cadeia', NULL, 'Online via Orienta Meet'),

-- Aula 4: CANCELADO com antecedência superior a 2h
(1004, 1001, 1003, 201, CURRENT_DATE - 1, '16:00:00'::time, 'CANCELADO', 'PRESENCIAL', 65.00, 'Limites Fundamentais e Continuidade', 'Conflito de horário com prova acadêmica comunicado com antecedência prévia.', 'Zona Sul / Moema')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status, valor_aplicado = EXCLUDED.valor_aplicado;

-- 6. Sessão da Sala de Aula e Eventos Auditáveis (Aula 1001)
INSERT INTO tb_sessao_aula (id, agendamento_id, aluno_checkin, professor_checkin, status_sessao, duracao_total_segundos, inicio_efetivo, fim_efetivo, aluno_confirmou_fim, professor_confirmou_fim)
VALUES (701, 1001, true, true, 'CONCLUIDA', 3600, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '1 hour', true, true)
ON CONFLICT (id) DO NOTHING;

-- 7. Avaliação Pós-Aula Registrada (Aula 1001)
INSERT INTO tb_avaliacoes (id, agendamento_id, avaliador_id, avaliado_id, nota, didatica, pontualidade, dominio, comentario, criada_em)
VALUES (801, 1001, 1001, 1002, 5, 5, 5, 5, 'Excelente aula! O professor explicou normalização e formas normais de maneira extremamente clara e prática.', NOW() - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;

-- 8. Módulo Financeiro: Transação Contábil e Solicitação de Saque PIX (Docente 1002)
-- Aula de R$ 70.00: Retenção institucional de 10% (R$ 7.00), Saldo Líquido de R$ 63.00
INSERT INTO tb_transacao_financeira (id, professor_id, agendamento_id, tipo_transacao, valor_bruto, taxa_plataforma, valor_liquido, descricao, data_transacao)
VALUES (901, 1002, 1001, 'CREDITO_AULA', 70.00, 7.00, 63.00, 'Liquidação de mentoria #1001 - Banco de Dados Relacional', NOW() - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_saque (id, professor_id, valor, taxa_aplicada, valor_liquido, status_saque, tipo_chave_pix, chave_pix, protocolo_saque, data_solicitacao, observacoes)
VALUES (951, 1002, 63.00, 0.00, 63.00, 'PENDENTE', 'EMAIL', 'prof.carlos@orienta.edu.br', 'SAQ-2026-88491', NOW() - INTERVAL '1 day', 'Solicitação de saque PIX via e-mail institucional')
ON CONFLICT (id) DO NOTHING;

-- 9. Chat Bilateral: Conversa e Mensagens com Confirmação de Leitura (Duplo Tique)
INSERT INTO tb_conversas (id, agendamento_id, ativa, criada_em, atualizada_em)
VALUES (101, 1001, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_mensagens (id, conversa_id, remetente_id, conteudo, tipo, lida, enviada_em)
VALUES
(1101, 101, 1001, 'Olá Professor Carlos! Já estou com a lista de exercícios de SQL pronta para a aula.', 'TEXTO', true, NOW() - INTERVAL '3 days' + INTERVAL '5 minutes'),
(1102, 101, 1002, 'Perfeito Gabriel! Vamos analisar os planos de execução de consulta e os índices.', 'TEXTO', true, NOW() - INTERVAL '3 days' + INTERVAL '7 minutes')
ON CONFLICT (id) DO NOTHING;

-- 10. Central de Suporte e Tickets com Protocolo Oficial SUP-YYYY-XXXXX
INSERT INTO tb_tickets_suporte (id, usuario_id, protocolo, titulo, assunto, descricao, categoria, prioridade, status, resposta, resposta_admin, criado_em)
VALUES
(1201, 1001, 'SUP-2026-10492', 'Dúvida sobre política de cancelamento de 2 horas', 'Dúvida sobre cancelamento', 'Gostaria de confirmar se o cancelamento com mais de 2 horas de antecedência não gera cobrança de multa.', 'DUVIDA', 'BAIXA', 'RESOLVIDO', 'A política da plataforma Orienta garante cancelamento sem ônus bilateral desde que solicitado com mais de 2 horas de antecedência da mentoria.', 'Atendimento concluído conforme regras da plataforma Orienta 2.0.', NOW() - INTERVAL '2 days'),
(1202, 1002, 'SUP-2026-10493', 'Prazo de liquidação do saque PIX', 'Prazo de saque PIX', 'Gostaria de saber a previsão de processamento do saque SAQ-2026-88491 solicitado via PIX.', 'FINANCEIRO', 'MEDIA', 'EM_ATENDIMENTO', 'Em análise financeira pelo setor responsável. Repasse programado em até 1 dia útil.', 'Chamado direcionado para o setor de pagamentos.', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- 11. Canal de Denúncias e Ouvidoria com Protocolo Oficial DEN-YYYY-XXXXX
INSERT INTO tb_denuncias (id, denunciante_id, denunciado_id, agendamento_id, protocolo, motivo, descricao, status, parecer_moderacao, parecer_moderador, criada_em, data_atualizacao)
VALUES
(1301, 1001, 1003, 1004, 'DEN-2026-55412', 'CONDUTA_INAPROPRIADA', 'Reporte teste de ouvidoria para auditoria e moderação ética da plataforma.', 'EM_ANALISE', 'Denúncia recebida pelo comitê de ética e em processo de análise preliminar.', 'Denúncia em processo de análise preliminar.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;
