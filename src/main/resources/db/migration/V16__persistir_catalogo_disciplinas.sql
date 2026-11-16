ALTER TABLE tb_saberes
    ADD COLUMN IF NOT EXISTS descricao TEXT,
    ADD COLUMN IF NOT EXISTS icone VARCHAR(80),
    ADD COLUMN IF NOT EXISTS topicos_populares JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS destaque BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS media_preco NUMERIC(10, 2);

INSERT INTO tb_saberes (nome, categoria, descricao, icone, topicos_populares, destaque, media_preco)
VALUES
('Cálculo Diferencial e Integral I', 'Exatas', 'Limites fundamentais, regras de derivação, regra da cadeia, otimização e integrais definidas.', 'activity', '["Limites Trigonométricos", "Derivadas Implícitas", "Área sob a Curva", "Taxas Relacionadas"]', TRUE, 45.00),
('Cálculo II e Multivariáveis', 'Exatas', 'Funções de várias variáveis, derivadas parciais, vetor gradiente, máximos e mínimos e integrais duplas.', 'trending-up', '["Gradiente e Derivada Direcional", "Multiplicadores de Lagrange", "Integrais Duplas em Polares"]', FALSE, 50.00),
('Álgebra Linear e Geometria Analítica', 'Exatas', 'Sistemas lineares, matrizes, determinantes, espaços vetoriais, autovalores e autovetores.', 'grid', '["Transformações Lineares", "Diagonalização de Matrizes", "Base e Dimensão", "Produto Escalar"]', TRUE, 45.00),
('Estatística Aplicada e Probabilidade', 'Exatas', 'Variáveis aleatórias, distribuições, testes de hipóteses e regressão linear.', 'bar-chart-2', '["Teorema Central do Limite", "Intervalos de Confiança", "Teste T de Student", "ANOVA"]', FALSE, 40.00),
('Física Mecânica e Dinâmica', 'Exatas', 'Cinemática vetorial, leis de Newton, trabalho e energia, rotação e momento angular.', 'compass', '["Conservação de Energia", "Colisões Elásticas", "Torque e Estática", "Gravitação"]', FALSE, 42.00),
('Matemática Financeira e Raciocínio Lógico', 'Exatas', 'Juros simples e compostos, taxas equivalentes, amortização e lógica proposicional.', 'dollar-sign', '["Tabela Price vs SAC", "Valor Presente Líquido", "Tabela Verdade", "Equivalências Lógicas"]', FALSE, 38.00),
('Estruturas de Dados e Algoritmos', 'Tecnologia', 'Vetores, listas encadeadas, pilhas, filas, árvores, grafos e análise assintótica.', 'cpu', '["Árvores Binárias de Busca", "Algoritmo de Dijkstra", "Busca em Largura", "Recursão e Pilha"]', TRUE, 50.00),
('Programação Orientada a Objetos com Java', 'Tecnologia', 'Classes, encapsulamento, herança, polimorfismo, interfaces, coleções e exceções.', 'code', '["Design Patterns", "Streams e Lambdas", "Generics", "Spring MVC e JPA"]', TRUE, 48.00),
('Banco de Dados Relacional e SQL', 'Tecnologia', 'Modelagem ER, normalização, DDL/DML, consultas, índices e transações.', 'database', '["Inner e Left Joins", "Agrupamentos com HAVING", "Triggers e Stored Procedures", "Otimização de Índices"]', FALSE, 45.00),
('Desenvolvimento Mobile com React Native', 'Tecnologia', 'Componentes nativos, Expo Router, hooks, navegação e integração com APIs REST.', 'smartphone', '["Expo Router", "Context API", "Estilização com StyleSheet", "AsyncStorage"]', TRUE, 52.00),
('Desenvolvimento Web Frontend Moderno', 'Tecnologia', 'HTML semântico, CSS, JavaScript, TypeScript, React e acessibilidade.', 'layout', '["Hooks do React", "CSS Grid e Flexbox", "Consumo com Axios e Fetch", "Acessibilidade"]', FALSE, 45.00),
('Inteligência Artificial e Machine Learning', 'Tecnologia', 'Regressão, classificação, árvores de decisão, redes neurais e processamento de linguagem.', 'zap', '["Scikit-Learn e Pandas", "Redes Neurais", "Validação Cruzada", "Processamento de Linguagem"]', FALSE, 55.00),
('Arquitetura de Computadores e Redes', 'Tecnologia', 'Hierarquia de memória, pipeline de CPU, modelos OSI e TCP/IP, roteamento e transporte.', 'hard-drive', '["Endereçamento IP", "TCP vs UDP", "Memória Cache", "Assembly"]', FALSE, 40.00),
('Bioquímica e Biologia Molecular', 'Ciências', 'Metabolismo, enzimas, DNA e síntese de proteínas.', 'crosshair', '["Cinética Enzimática", "Glicólise e Fermentação", "Replicação do DNA", "Aminoácidos"]', FALSE, 42.00),
('Anatomia e Fisiologia Humana', 'Ciências', 'Sistemas cardiovascular, respiratório, nervoso, digestório e renal.', 'heart', '["Potencial de Ação Neural", "Circulação", "Filtração Glomerular", "Músculo Esquelético"]', FALSE, 45.00),
('Química Geral e Orgânica', 'Ciências', 'Estequiometria, equilíbrio químico, soluções, funções orgânicas e mecanismos de reação.', 'feather', '["Cálculos Estequiométricos", "pH e Soluções Tampão", "Reações Orgânicas", "Cinética Química"]', TRUE, 40.00),
('Metodologia Científica e TCC', 'Humanas', 'Estruturação de artigos, revisão bibliográfica, normas acadêmicas e defesa de monografia.', 'file-text', '["Normas ABNT", "Delimitação do Tema", "Revisão de Literatura", "Oratória para Banca"]', TRUE, 46.00),
('Direito Constitucional e Administrativo', 'Humanas', 'Direitos fundamentais, organização dos poderes, atos administrativos e licitações.', 'shield', '["Controle de Constitucionalidade", "Poder de Polícia", "Administração Pública", "Ação Popular"]', FALSE, 48.00),
('Filosofia e Sociologia Contemporânea', 'Humanas', 'Teorias clássicas e contemporâneas, ética, política e sociedade digital.', 'book', '["Escola de Frankfurt", "Ética", "Sociedade Digital", "Contratualismo"]', FALSE, 38.00),
('Inglês Acadêmico e Conversação', 'Idiomas', 'Fluência verbal, vocabulário técnico, leitura científica e redação de abstracts.', 'globe', '["Apresentações", "TOEFL e IELTS", "Leitura de Artigos", "Pronúncia"]', TRUE, 45.00),
('Espanhol para Fins Acadêmicos e Profissionais', 'Idiomas', 'Gramática, leitura instrumental, conversação profissional e preparação para exames.', 'message-circle', '["Falsos Cognatos", "Subjuntivo", "Redação", "Prática Auditiva"]', FALSE, 38.00),
('Comunicação, Redação e Oratória', 'Idiomas', 'Coesão textual, argumentação, postura corporal e comunicação oral.', 'mic', '["Argumentação", "Vícios de Linguagem", "Apresentações", "Storytelling"]', FALSE, 40.00),
('Gestão Financeira e Controladoria', 'Negócios', 'Análise de demonstrativos, fluxo de caixa, capital de giro e valuation.', 'pie-chart', '["EBITDA e Margens", "Ponto de Equilíbrio", "Análise Financeira", "Gestão de Custos"]', FALSE, 45.00),
('Gestão de Projetos e Metodologias Ágeis', 'Negócios', 'Scrum, Kanban, estimativas, métricas de fluxo e OKRs.', 'check-circle', '["Sprints", "User Stories", "Burn-down Charts", "Kanban"]', TRUE, 48.00),
('Marketing Digital e Growth Estratégico', 'Negócios', 'Aquisição, inbound marketing, métricas CAC/LTV, SEO e retenção.', 'share-2', '["Funil de Conversão", "CAC e LTV", "SEO", "Testes A/B"]', FALSE, 42.00)
ON CONFLICT (nome) DO UPDATE SET
    categoria = EXCLUDED.categoria,
    descricao = EXCLUDED.descricao,
    icone = EXCLUDED.icone,
    topicos_populares = EXCLUDED.topicos_populares,
    destaque = EXCLUDED.destaque,
    media_preco = EXCLUDED.media_preco;
