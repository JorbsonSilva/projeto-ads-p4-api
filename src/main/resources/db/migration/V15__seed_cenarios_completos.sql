-- ==============================================================================
-- ORIENTA 2.0 - MIGRATION V15: SEED COMPLETO E DEFINITIVO DE CENÁRIOS DE DADOS
-- Rastreabilidade: Tarefa [MIG-001]
-- Descrição: Transfere e consolida todos os dados que antes residiam em simulações
-- do frontend (demoData.ts, bancaMockData.ts e fallbacks de serviços) diretamente
-- para o banco de dados relacional PostgreSQL.
-- Todos os comandos usam ON CONFLICT DO UPDATE/NOTHING para garantir IDEMPOTÊNCIA.
-- ==============================================================================

-- 1. Inserção / Atualização dos Usuários (Alunos, Professores e Admin)
INSERT INTO tb_usuarios (
    id, nome, email, senha, tipo, papel, cpf, email_verificado, 
    bio_aluno, bio_professor, foto_url, nota_media, total_avaliacoes, 
    oferece_presencial, regiao_atendimento, latitude_aproximada, longitude_aproximada, raio_atendimento_km
) VALUES
-- Aluno Principal (Banca e Homologação)
(1001, 'Gabriel Aluno (Banca)', 'aluno.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ALUNO', 'ROLE_ALUNO', '111.222.333-44', true, 'Estudante de Engenharia de Software no 4º período. Foco em Banco de Dados, Algoritmos e Mobile.', NULL, 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL),

-- Alunos Adicionais
(1, 'Carlos Aluno', 'carlos@aluno.orienta.com.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ALUNO', 'ROLE_ALUNO', '123.456.789-01', true, 'Estudante do 4º período de ADS. Apaixonado por desenvolvimento backend.', NULL, 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL),
(2, 'Mariana Santos', 'mariana@aluno.orienta.com.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ALUNO', 'ROLE_ALUNO', '987.654.321-09', true, 'Estudante de Engenharia de Software. Foco em Mobile e UX.', NULL, 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL),

-- Professores de Destaque da Simulação (Migrados para o Banco)
(101, 'Prof. Lucas Rocha', 'lucas.rocha@orienta.com.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '555.666.777-88', true, NULL, 'Especialista em Matemática Universitária e Cálculo. Metodologia ativa e resolução guiada de exercícios.', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400', 4.9, 128, true, 'Campus Central / Online', -23.555000, -46.638000, 10),
(102, 'Profa. Juliana Santos', 'juliana.santos@orienta.com.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '666.777.888-99', true, NULL, 'Mestre em Física Aplicada. Didática visual para Mecânica, Eletromagnetismo e Termodinâmica.', 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=400', 4.8, 96, true, 'Zona Sul / Online', -23.601000, -46.662000, 15),
(103, 'Prof. Rafael Kim', 'rafael.kim@orienta.com.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '777.888.999-00', true, NULL, 'Engenheiro de Software Sênior com 8 anos de experiência com ecossistema Java, Spring e Arquitetura de Microsserviços.', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 4.9, 74, false, 'Sala Virtual Exclusiva', -23.560000, -46.650000, 20),

-- Professores Oficiais de Banca
(1002, 'Prof. Dr. Carlos Eduardo', 'prof.carlos@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '222.333.444-55', true, NULL, 'Doutor em Ciência da Computação (UFSCar). Mais de 10 anos lecionando Bancos de Dados Relacionais e Arquitetura de Software.', 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=400', 4.9, 38, true, 'Centro / Bela Vista', -23.550520, -46.633308, 10),
(1003, 'Profa. Ma. Juliana Oliveira', 'profa.juliana@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'PROFESSOR', 'ROLE_PROFESSOR', '333.444.555-66', true, NULL, 'Mestre em Matemática Pura (USP). Especialista no desbloqueio pedagógico em Cálculo I, II e Álgebra Linear.', 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=400', 5.0, 45, true, 'Zona Sul / Moema', -23.600000, -46.660000, 15),

-- Administrador / Ouvidoria
(1004, 'Mariana Coordenadora (Admin)', 'admin.banca@orienta.edu.br', '$2a$10$Xp7Z06qI7bU.3z/e2rC1qOBZzRfx6ZfF.9tB.8eUjVj1v5Uf9k5wO', 'ADMIN', 'ROLE_ADMIN', '444.555.666-77', true, NULL, NULL, 'https://images.unsplash.com/photo-1580894732454-defa10bcfdbd?w=400', 5.0, 0, false, NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO UPDATE SET
    nome = EXCLUDED.nome,
    email = EXCLUDED.email,
    papel = EXCLUDED.papel,
    tipo = EXCLUDED.tipo,
    bio_professor = EXCLUDED.bio_professor,
    foto_url = EXCLUDED.foto_url,
    nota_media = EXCLUDED.nota_media,
    total_avaliacoes = EXCLUDED.total_avaliacoes,
    oferece_presencial = EXCLUDED.oferece_presencial,
    regiao_atendimento = EXCLUDED.regiao_atendimento,
    latitude_aproximada = EXCLUDED.latitude_aproximada,
    longitude_aproximada = EXCLUDED.longitude_aproximada,
    raio_atendimento_km = EXCLUDED.raio_atendimento_km;

-- 2. Catálogo Acadêmico Completo de Disciplinas (tb_saberes)
INSERT INTO tb_saberes (id, nome, categoria) VALUES
(1, 'Matemática Geral', 'Exatas'),
(2, 'Programação Java', 'Tecnologia'),
(3, 'Física Geral', 'Exatas'),
(4, 'Química Orgânica', 'Biológicas'),
(5, 'Cálculo Diferencial', 'Exatas'),
(6, 'Inglês Instrumental', 'Linguagens'),
(201, 'Cálculo Diferencial e Integral I', 'Matemática e Exatas'),
(202, 'Banco de Dados Relacional e SQL', 'Engenharia de Software'),
(203, 'Estruturas de Dados e Algoritmos', 'Ciência da Computação'),
(204, 'Desenvolvimento Mobile com React Native', 'Mobile'),
(205, 'Arquitetura de Software e Microsserviços', 'Engenharia de Software')
ON CONFLICT (id) DO UPDATE SET 
    nome = EXCLUDED.nome, 
    categoria = EXCLUDED.categoria;

-- 3. Aptidões e Preço-Hora de Cada Professor
INSERT INTO tb_aptidoes (id, usuario_id, saber_id, preco_hora, nivel_dominio) VALUES
(101, 101, 1, 60.00, 'Avançado'),
(102, 101, 5, 70.00, 'Especialista'),
(103, 102, 3, 55.00, 'Mestrado'),
(104, 102, 4, 60.00, 'Avançado'),
(105, 103, 2, 75.00, 'Sênior'),
(106, 103, 202, 80.00, 'Especialista'),
(501, 1002, 202, 70.00, 'Doutorado'),
(502, 1002, 203, 75.00, 'Avançado'),
(503, 1003, 201, 65.00, 'Mestrado')
ON CONFLICT (id) DO UPDATE SET 
    preco_hora = EXCLUDED.preco_hora,
    nivel_dominio = EXCLUDED.nivel_dominio;

-- 4. Disponibilidade Semanal dos Professores
INSERT INTO tb_disponibilidade (id, usuario_id, dia_semana, hora_inicio, hora_fim) VALUES
(101, 101, 'SEGUNDA', '08:00:00', '18:00:00'),
(102, 101, 'QUARTA', '10:00:00', '20:00:00'),
(103, 101, 'SEXTA', '08:00:00', '17:00:00'),
(104, 102, 'TERCA', '09:00:00', '18:00:00'),
(105, 102, 'QUINTA', '09:00:00', '19:00:00'),
(106, 103, 'SEGUNDA', '13:00:00', '21:00:00'),
(107, 103, 'QUARTA', '13:00:00', '21:00:00'),
(601, 1002, 'SEGUNDA', '08:00:00', '18:00:00'),
(602, 1002, 'QUARTA', '14:00:00', '20:00:00'),
(603, 1003, 'TERCA', '09:00:00', '17:00:00'),
(604, 1003, 'QUINTA', '10:00:00', '19:00:00')
ON CONFLICT (id) DO NOTHING;

-- 5. Agendamentos em Múltiplos Estados Reais (Auditáveis)
INSERT INTO tb_agendamento (
    id, id_aluno, id_professor, saber_id, data, hora, 
    status, modalidade, valor_aplicado, assunto, justificativa_cancelamento, regiao_ou_local_referencia
) VALUES
-- Aula Canônica do Checkout e Marcar Aula (IMG-008)
(501, 1001, 101, 1, CURRENT_DATE + 2, '14:00:00'::time, 'CONFIRMADO', 'ONLINE', 60.00, 'Matemática e Funções Exponenciais', NULL, 'Online via Orienta Meet'),

-- Aula 201: Matemática - Confirmada
(201, 1001, 101, 1, CURRENT_DATE + 3, '14:00:00'::time, 'CONFIRMADO', 'ONLINE', 60.00, 'Revisão Geral de Trigonometria e Geometria', NULL, 'Online via Orienta Meet'),

-- Aula 202: Física Geral - Pendente aguardando aceite docente
(202, 1001, 102, 3, CURRENT_DATE + 4, '10:00:00'::time, 'PENDENTE', 'PRESENCIAL', 55.00, 'Leis de Newton e Dinâmica de Corpos Rígidos', NULL, 'Zona Sul / Online'),

-- Aula 203: Programação Java - Realizada
(203, 1001, 103, 2, CURRENT_DATE - 4, '16:00:00'::time, 'REALIZADA', 'ONLINE', 75.00, 'Programação Orientada a Objetos com Java', NULL, 'Sala Virtual Exclusiva'),

-- Aula 204: Cálculo Diferencial - Cancelado bilateralmente com antecedência
(204, 1001, 101, 5, CURRENT_DATE - 2, '09:00:00'::time, 'CANCELADO', 'ONLINE', 70.00, 'Derivadas Parciais e Aplicações', 'Cancelamento solicitado com antecedência de 24h pelo aluno.', 'Online via Orienta Meet'),

-- Aulas Oficiais de Banca
(1001, 1001, 1002, 202, CURRENT_DATE - 3, '14:00:00'::time, 'REALIZADA', 'ONLINE', 70.00, 'Normalização de Dados e Índices B-Tree', NULL, 'Online via Orienta Meet'),
(1002, 1001, 1002, 203, CURRENT_DATE + 2, '15:00:00'::time, 'CONFIRMADO', 'PRESENCIAL', 75.00, 'Árvores Binárias de Busca e Grafos', NULL, 'Centro / Bela Vista'),
(1003, 1001, 1003, 201, CURRENT_DATE + 5, '10:00:00'::time, 'PENDENTE', 'ONLINE', 65.00, 'Revisão de Derivadas e Regra da Cadeia', NULL, 'Online via Orienta Meet'),
(1004, 1001, 1003, 201, CURRENT_DATE - 1, '16:00:00'::time, 'CANCELADO', 'PRESENCIAL', 65.00, 'Limites Fundamentais e Continuidade', 'Conflito de horário com prova acadêmica comunicado previamente.', 'Zona Sul / Moema')
ON CONFLICT (id) DO UPDATE SET 
    status = EXCLUDED.status, 
    valor_aplicado = EXCLUDED.valor_aplicado,
    data = EXCLUDED.data,
    hora = EXCLUDED.hora;

-- 6. Sessão da Sala de Aula e Eventos Auditáveis Anti-Fraude
INSERT INTO tb_sessao_aula (
    id, agendamento_id, aluno_checkin, professor_checkin, 
    status_sessao, duracao_total_segundos, inicio_efetivo, fim_efetivo, 
    aluno_confirmou_fim, professor_confirmou_fim
) VALUES 
(701, 1001, true, true, 'CONCLUIDA', 3600, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '1 hour', true, true),
(702, 203, true, true, 'CONCLUIDA', 3600, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '1 hour', true, true),
(703, 501, false, false, 'AGUARDANDO_INICIO', 0, NULL, NULL, false, false)
ON CONFLICT (id) DO NOTHING;

-- 7. Avaliações Pós-Aula Registradas no Banco
INSERT INTO tb_avaliacoes (
    id, agendamento_id, avaliador_id, avaliado_id, 
    nota, didatica, pontualidade, dominio, comentario, criada_em
) VALUES
(801, 1001, 1001, 1002, 5, 5, 5, 5, 'Excelente aula! O professor explicou normalização e formas normais de maneira extremamente clara e prática.', NOW() - INTERVAL '3 days'),
(802, 203, 1001, 103, 5, 5, 5, 5, 'Professor muito didático! Desmistificou polimorfismo e interfaces no Java moderno.', NOW() - INTERVAL '4 days')
ON CONFLICT (id) DO NOTHING;

-- 8. Módulo Financeiro: Transações Contábeis e Saques PIX
-- Regra Institucional: Taxa de retenção de 10%, saldo líquido de 90% para o docente
INSERT INTO tb_transacao_financeira (
    id, professor_id, agendamento_id, tipo_transacao, 
    valor_bruto, taxa_plataforma, valor_liquido, descricao, data_transacao
) VALUES
-- Transações do Prof. Carlos Eduardo (1002)
(901, 1002, 1001, 'CREDITO_AULA', 70.00, 7.00, 63.00, 'Liquidação de mentoria #1001 - Banco de Dados Relacional', NOW() - INTERVAL '3 days'),

-- Transações do Prof. Rafael Kim (103)
(902, 103, 203, 'CREDITO_AULA', 75.00, 7.50, 67.50, 'Liquidação de mentoria #203 - Programação Java', NOW() - INTERVAL '4 days'),

-- Transações do Prof. Lucas Rocha (101)
(903, 101, 501, 'CREDITO_AULA', 60.00, 6.00, 54.00, 'Liquidação de mentoria #501 - Matemática', NOW() - INTERVAL '10 days')
ON CONFLICT (id) DO NOTHING;

-- Solicitações de Saque Docente (com protocolo oficial SAQ-YYYY-XXXXX)
INSERT INTO tb_saque (
    id, professor_id, valor, taxa_aplicada, valor_liquido, 
    status_saque, tipo_chave_pix, chave_pix, protocolo_saque, data_solicitacao, observacoes
) VALUES
(951, 1002, 63.00, 0.00, 63.00, 'PENDENTE', 'EMAIL', 'prof.carlos@orienta.edu.br', 'SAQ-2026-88491', NOW() - INTERVAL '1 day', 'Solicitação de saque PIX via e-mail institucional'),
(952, 101, 54.00, 0.00, 54.00, 'PAGO', 'CPF', '555.666.777-88', 'SAQ-2026-12948', NOW() - INTERVAL '8 days', 'Saque processado e liquidado via Pix pelo setor financeiro')
ON CONFLICT (id) DO NOTHING;

-- 9. Chat Bilateral: Conversas e Histórico de Mensagens
INSERT INTO tb_conversas (id, agendamento_id, ativa, criada_em, atualizada_em) VALUES
(101, 1001, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
(102, 501, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
(103, 203, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_mensagens (id, conversa_id, remetente_id, conteudo, tipo, lida, enviada_em) VALUES
(1101, 101, 1001, 'Olá Professor Carlos! Já estou com a lista de exercícios de SQL pronta para a aula.', 'TEXTO', true, NOW() - INTERVAL '3 days' + INTERVAL '5 minutes'),
(1102, 101, 1002, 'Perfeito Gabriel! Vamos analisar os planos de execução de consulta e os índices.', 'TEXTO', true, NOW() - INTERVAL '3 days' + INTERVAL '7 minutes'),
(1103, 102, 1001, 'Boa tarde Prof. Lucas! Gostaria de focar a mentoria em funções trigonométricas.', 'TEXTO', true, NOW() - INTERVAL '2 days'),
(1104, 102, 101, 'Excelente escolha, Gabriel! Preparei um resumo gráfico de seno e cosseno para analisarmos.', 'TEXTO', true, NOW() - INTERVAL '2 days' + INTERVAL '10 minutes')
ON CONFLICT (id) DO NOTHING;

-- 10. Central de Suporte e Tickets com Protocolo Oficial SUP-YYYY-XXXXX
INSERT INTO tb_tickets_suporte (
    id, usuario_id, protocolo, titulo, assunto, descricao, 
    categoria, prioridade, status, resposta, resposta_admin, criado_em
) VALUES
(1201, 1001, 'SUP-2026-10492', 'Dúvida sobre política de cancelamento de 2 horas', 'Dúvida sobre cancelamento', 'Gostaria de confirmar se o cancelamento com mais de 2 horas de antecedência não gera cobrança de multa.', 'DUVIDA', 'BAIXA', 'RESOLVIDO', 'A política da plataforma Orienta garante cancelamento sem ônus bilateral desde que solicitado com mais de 2 horas de antecedência da mentoria.', 'Atendimento concluído conforme regras da plataforma Orienta 2.0.', NOW() - INTERVAL '2 days'),
(1202, 1002, 'SUP-2026-10493', 'Prazo de liquidação do saque PIX', 'Prazo de saque PIX', 'Gostaria de saber a previsão de processamento do saque SAQ-2026-88491 solicitado via PIX.', 'FINANCEIRO', 'MEDIA', 'EM_ATENDIMENTO', 'Em análise financeira pelo setor responsável. Repasse programado em até 1 dia útil.', 'Chamado direcionado para o setor de pagamentos.', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- 11. Canal de Denúncias e Ouvidoria com Protocolo Oficial DEN-YYYY-XXXXX
INSERT INTO tb_denuncias (
    id, denunciante_id, denunciado_id, agendamento_id, protocolo, 
    motivo, descricao, status, parecer_moderacao, parecer_moderador, criada_em, data_atualizacao
) VALUES
(1301, 1001, 1003, 1004, 'DEN-2026-55412', 'CONDUTA_INAPROPRIADA', 'Reporte teste de ouvidoria para auditoria e moderação ética da plataforma.', 'EM_ANALISE', 'Denúncia recebida pelo comitê de ética e em processo de análise preliminar.', 'Denúncia em processo de análise preliminar.', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

-- 12. Notificações do Sistema para Usuários (Central de Notificações)
INSERT INTO tb_notificacoes (id, usuario_id, titulo, mensagem, tipo, lida, data_hora, referencia_id) VALUES
(1, 1001, 'Aula Confirmada!', 'Prof. Lucas Rocha aceitou sua solicitação de mentoria de Matemática para quarta-feira.', 'AGENDAMENTO', true, NOW() - INTERVAL '2 days', 501),
(2, 1001, 'Nova Mensagem', 'Prof. Dr. Carlos Eduardo enviou uma nova mensagem no chat sobre Normalização.', 'CHAT', false, NOW() - INTERVAL '1 day', 101),
(3, 1001, 'Lembrete de Aula', 'Sua mentoria de Matemática com Prof. Lucas Rocha começa em breve.', 'SISTEMA', false, NOW() - INTERVAL '2 hours', 501),
(4, 101, 'Novo Agendamento Recebido', 'Você recebeu uma solicitação de mentoria para Matemática.', 'AGENDAMENTO', true, NOW() - INTERVAL '2 days', 501),
(5, 1002, 'Saque Solicitado', 'Sua solicitação de saque SAQ-2026-88491 no valor de R$ 63,00 foi registrada.', 'SUPORTE', false, NOW() - INTERVAL '1 day', 951)
ON CONFLICT (id) DO NOTHING;
