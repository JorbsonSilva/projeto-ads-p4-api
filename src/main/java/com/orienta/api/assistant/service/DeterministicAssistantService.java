package com.orienta.api.assistant.service;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.SaberRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import com.orienta.api.assistant.dto.*;
import com.orienta.api.assistant.model.TriageStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de Triagem e Assistente Pedagógico Determinístico do Sistema Orienta.
 * Rastreabilidade: Fase 7 - Bloco L (Tarefa [BACK-018]) / Requisito RN-001
 *
 * PRINCÍPIOS ARQUITETURAIS:
 * 1. Zero Custo de API Externa: Não depende de APIs pagas ou provedores externos instáveis.
 * 2. Zero Alucinação: Respostas fundamentadas estritamente em matrizes pedagógicas e catálogo acadêmico.
 * 3. Contingência e Resiliência: Fornece respostas pedagógicas estruturadas por chips com suporte a fórmulas,
 *    código-fonte conceitual e listas de tópicos.
 * 4. Apenas Leitura: Anotado com @Transactional(readOnly = true) — bloqueado contra criação direta de agendamentos.
 * 5. Fallback Seguro: Mensagens fora de escopo são encaminhadas com protocolo de suporte auditável SUP-YYYY-XXXXX.
 */
@Service
@Transactional(readOnly = true)
public class DeterministicAssistantService {

    private static final Logger log = LoggerFactory.getLogger(DeterministicAssistantService.class);

    private final UsuarioRepository usuarioRepository;
    private final SaberRepository saberRepository;

    // Catálogo determinístico de disciplinas e assuntos homologados
    private static final Map<String, List<String>> CATALOGO_DISCIPLINAS = new LinkedHashMap<>();

    static {
        CATALOGO_DISCIPLINAS.put("Cálculo I", List.of("Limites e Continuidade", "Derivadas e Regra da Cadeia", "Integrais Definidas", "Aplicações de Otimização"));
        CATALOGO_DISCIPLINAS.put("Estrutura de Dados", List.of("Vetores e Listas Ligadas", "Pilhas e Filas", "Árvores Binárias e AVL", "Grafos e Algoritmos de Dijkstra"));
        CATALOGO_DISCIPLINAS.put("Banco de Dados", List.of("Modelo Entidade-Relacionamento", "Normalização (1FN, 2FN, 3FN)", "Consultas SQL Avançadas e Joins", "Transações ACID e Índices"));
        CATALOGO_DISCIPLINAS.put("Programação Orientada a Objetos", List.of("Encapsulamento e Modificadores", "Herança e Polimorfismo", "Interfaces e Classes Abstratas", "Tratamento de Exceções"));
        CATALOGO_DISCIPLINAS.put("Redes de Computadores", List.of("Modelo OSI e TCP/IP", "Sub-redes e Máscaras IPv4", "Protocolos DNS, DHCP e HTTP", "Segurança e Roteamento"));
    }

    // Base demonstrativa de contingência de professores ativos cadastrados na plataforma
    private static final List<ProfessorResumoDTO> PROFESSORES_CATALOGO = new ArrayList<>();

    static {
        PROFESSORES_CATALOGO.add(new ProfessorResumoDTO(
                101L, "Prof. Dr. Marcelo Santos", "Doutor em Ciência da Computação",
                new BigDecimal("65.00"), 4.9, 38,
                List.of("ONLINE", "PRESENCIAL"), "Centro / Asa Norte",
                List.of("Estrutura de Dados", "Programação Orientada a Objetos"), true
        ));

        PROFESSORES_CATALOGO.add(new ProfessorResumoDTO(
                102L, "Profa. Ma. Juliana Oliveira", "Mestre em Matemática Aplicada",
                new BigDecimal("55.00"), 4.8, 42,
                List.of("ONLINE"), "Águas Claras",
                List.of("Cálculo I"), true
        ));

        PROFESSORES_CATALOGO.add(new ProfessorResumoDTO(
                103L, "Prof. Carlos Eduardo Lima", "Especialista em Engenharia de Dados",
                new BigDecimal("70.00"), 5.0, 19,
                List.of("ONLINE", "PRESENCIAL"), "Taguatinga Centro",
                List.of("Banco de Dados", "Programação Orientada a Objetos"), true
        ));

        PROFESSORES_CATALOGO.add(new ProfessorResumoDTO(
                104L, "Profa. Beatriz Rocha", "Mestre em Engenharia Elétrica e Redes",
                new BigDecimal("50.00"), 4.7, 27,
                List.of("ONLINE", "PRESENCIAL"), "Sudoeste",
                List.of("Redes de Computadores"), true
        ));

        PROFESSORES_CATALOGO.add(new ProfessorResumoDTO(
                105L, "Prof. Lucas Mendes", "Bacharel em Ciência da Computação",
                new BigDecimal("40.00"), 4.6, 15,
                List.of("ONLINE"), "Guará",
                List.of("Cálculo I", "Estrutura de Dados"), true
        ));
    }

    public DeterministicAssistantService() {
        this.usuarioRepository = null;
        this.saberRepository = null;
    }

    @Autowired
    public DeterministicAssistantService(
            @Autowired(required = false) UsuarioRepository usuarioRepository,
            @Autowired(required = false) SaberRepository saberRepository) {
        this.usuarioRepository = usuarioRepository;
        this.saberRepository = saberRepository;
    }

    /**
     * Processa um passo da triagem determinística com base na máquina de estados finitos ou chip rápido.
     */
    public TriageResponseDTO processStep(TriageRequestDTO request) {
        TriageStep current = request.getCurrentStep() != null ? request.getCurrentStep() : TriageStep.INITIAL;
        log.info("Processando triagem determinística no passo: {}", current);

        String userMsg = request.getUserMessage();

        // 1. Detecção de mensagens fora de escopo (F9-T5)
        if (isOutOfScopeQuery(userMsg)) {
            return buildSupportHandoffResponse(userMsg);
        }

        // 2. Se a requisição contiver chip pedagógico explícito ou na mensagem, processa pedagogicamente
        if (request.getChipTipo() != null || isPedagogicChipQuery(userMsg)) {
            PerguntaAssistenteDTO perguntaDto = new PerguntaAssistenteDTO();
            perguntaDto.setPergunta(userMsg != null ? userMsg : "Orientação sobre disciplina");
            perguntaDto.setDisciplina(request.getDisciplinaNome());
            perguntaDto.setAssunto(request.getAssuntoNome());
            perguntaDto.setChipTipo(request.getChipTipo());
            perguntaDto.setModalidade(request.getModalidade());
            perguntaDto.setOrcamentoMaximo(request.getOrcamentoMaximo());
            return processarPergunta(perguntaDto, null);
        }

        switch (current) {
            case INITIAL:
                return handleInitialStep();

            case SUBJECT_SELECTION:
                return handleSubjectSelection(request);

            case TOPIC_SELECTION:
                return handleTopicSelection(request);

            case MODALITY_BUDGET:
                return handleModalityAndBudget(request);

            case RECOMMENDATION:
                return handleRecommendationFinalization(request);

            default:
                return handleInitialStep();
        }
    }

    /**
     * Processa perguntas livres contextualizadas com disciplinas/saberes acadêmicos e chips pedagógicos rápidos.
     * Rastreabilidade: Tarefa [BACK-018]
     */
    public TriageResponseDTO processarPergunta(PerguntaAssistenteDTO request, Long usuarioId) {
        String texto = request.getPergunta() != null ? request.getPergunta().trim() : "";
        log.info("Processando pergunta livre/chip no assistente. Usuário: {}, Pergunta: [{}]", usuarioId, texto);

        // 1. Verificação de fora do escopo
        if (isOutOfScopeQuery(texto)) {
            return buildSupportHandoffResponse(texto);
        }

        // 2. Identificação da disciplina alvo
        String disciplina = identificarDisciplina(request.getDisciplina(), texto);
        String assunto = request.getAssunto() != null ? request.getAssunto() : "Dúvidas e Aplicações Práticas";

        // 3. Identificação do chip ou intenção pedagógica
        String chipTipo = normalizarChipTipo(request.getChipTipo(), texto);

        TriageResponseDTO response;

        switch (chipTipo) {
            case "EXPLICAR_EXERCICIO":
                response = buildExplicarExercicioResponse(disciplina, assunto, texto);
                break;

            case "GERAR_RESUMO":
                response = buildGerarResumoResponse(disciplina, assunto);
                break;

            case "PLANO_ESTUDO":
                response = buildPlanoEstudosResponse(disciplina, assunto);
                break;

            case "DICAS_PROVA":
                response = buildDicasProvaResponse(disciplina, assunto);
                break;

            default:
                response = buildRespostaGeralContextualizada(disciplina, assunto, texto);
                break;
        }

        // 4. Associação de professores compatíveis
        List<ProfessorResumoDTO> professores = buscarProfessoresRecomendados(disciplina, request.getModalidade(), request.getOrcamentoMaximo());
        response.setRecommendedTeachers(professores);

        if (!professores.isEmpty()) {
            ProfessorResumoDTO topProf = professores.get(0);
            ActionCardDTO actionCard = new ActionCardDTO(
                    topProf.getId(),
                    topProf.getNome(),
                    disciplina,
                    assunto,
                    topProf.getModalidades().contains("PRESENCIAL") ? "Presencial / Online" : "Online",
                    topProf.getValorHora(),
                    "/agendamento/" + topProf.getId()
            );
            response.setActionCard(actionCard);
        }

        response.setDeterministicRuleBased(true);
        response.setRuleReferenceCode("RULE_PEDAGOGIC_FALLBACK_V9");
        return response;
    }

    /**
     * Resposta determinística para o chip 'Explicar Exercício'.
     */
    private TriageResponseDTO buildExplicarExercicioResponse(String disciplina, String assunto, String textoOriginal) {
        TriageResponseDTO response = new TriageResponseDTO();
        response.setNextStep(TriageStep.RECOMMENDATION);

        if ("Cálculo I".equalsIgnoreCase(disciplina) || textoOriginal.toLowerCase().contains("integral") || textoOriginal.toLowerCase().contains("calculo")) {
            response.setAssistantMessage("Vamos resolver passo a passo! Para integrais por partes, aplicamos a regra fundamental deduzida da diferenciação do produto:");
            response.setFormula("∫ u dv = u·v - ∫ v du\n\nRegra de Prioridade (LIATE):\n1. Logarítmicas (L)\n2. Inversas Trigonométricas (I)\n3. Algébricas (A)\n4. Trigonométricas (T)\n5. Exponenciais (E)");
            response.setBulletPoints(List.of(
                    "1. Identifique a função de maior prioridade pelo método LIATE e atribua a 'u'",
                    "2. Atribua o restante do integrando a 'dv'",
                    "3. Calcule du = u' dx e determine v integrando dv",
                    "4. Aplique os valores na fórmula padrão e resolva a integral remanescente ∫ v du"
            ));
        } else if ("Estrutura de Dados".equalsIgnoreCase(disciplina) || textoOriginal.toLowerCase().contains("arvore") || textoOriginal.toLowerCase().contains("dados")) {
            response.setAssistantMessage("Vamos analisar a resolução analítica para manipulação de nós em Árvores Binárias:");
            response.setCodeSnippet("int contarNos(Nodo raiz) {\n    if (raiz == null) return 0;\n    return 1 + contarNos(raiz.esquerda) + contarNos(raiz.direita);\n}");
            response.setBulletPoints(List.of(
                    "1. Caso Base: Árvore ou subárvore vazia retorna contagem 0",
                    "2. Passo Recursivo: Soma 1 (nó atual) às chamadas recursivas das subárvores esquerda e direita",
                    "3. Complexidade Temporal: O(n) onde n é o total de elementos da árvore"
            ));
        } else {
            response.setAssistantMessage("Excelente questão em " + disciplina + "! Para resolver exercícios dessa matéria com rigor metódico, siga as etapas abaixo:");
            response.setBulletPoints(List.of(
                    "1. Mapeie todas as variáveis de entrada, restrições e unidades de medida",
                    "2. Isole as equações ou axiomas fundamentais pertinentes ao assunto",
                    "3. Desenvolva o cálculo ou algoritmo passo a passo sem pular fases intermediárias",
                    "4. Faça a verificação dimensional ou trace de execução com casos de teste simples"
            ));
        }

        response.setQuickOptions(List.of(
                new QuickOptionDTO("chip_outro_ex", "📐 Ver outro exercício similar", "EXPLICAR_EXERCICIO", "Outra demonstração prática"),
                new QuickOptionDTO("chip_resumo", "📝 Gerar Resumo Teórico", "GERAR_RESUMO", "Síntese dos axiomas e regras"),
                new QuickOptionDTO("chip_mentor", "👨‍🏫 Agendar Mentoria Individual", "AGENDAR_MENTORIA", "Prática assistida com professor")
        ));

        return response;
    }

    /**
     * Resposta determinística para o chip 'Gerar Resumo'.
     */
    private TriageResponseDTO buildGerarResumoResponse(String disciplina, String assunto) {
        TriageResponseDTO response = new TriageResponseDTO();
        response.setNextStep(TriageStep.RECOMMENDATION);

        if ("Estrutura de Dados".equalsIgnoreCase(disciplina)) {
            response.setAssistantMessage("Aqui está a síntese teórica estruturada de Árvores Binárias de Busca (BST):");
            response.setCodeSnippet("// Propriedade invariante da BST:\n// nodo.esquerda.chave < nodo.chave <= nodo.direita.chave\n\nclass Nodo {\n    int valor;\n    Nodo esquerda;\n    Nodo direita;\n}");
            response.setBulletPoints(List.of(
                    "Complexidade Média de Busca, Inserção e Remoção: O(log n)",
                    "Pior Caso (Árvore degenerada em lista): O(n) — corrigido com árvores AVL ou Red-Black",
                    "Percurso Em-Ordem (In-Order): Percorre e exibe os elementos em ordem estritamente crescente"
            ));
        } else if ("Banco de Dados".equalsIgnoreCase(disciplina)) {
            response.setAssistantMessage("Aqui está o resumo dos princípios de Normalização de Dados (1FN, 2FN e 3FN):");
            response.setBulletPoints(List.of(
                    "1FN (Primeira Forma Normal): Todos os atributos devem ser atômicos (sem campos multivalorados ou listas)",
                    "2FN (Segunda Forma Normal): Estar na 1FN e nenhum atributo não-chave depender parcialmente da chave primária composta",
                    "3FN (Terceira Forma Normal): Estar na 2FN e nenhum atributo não-chave depender transitivamente de outro atributo não-chave",
                    "Propriedades ACID: Atomicidade, Consistência, Isolamento e Durabilidade nas transações relacionais"
            ));
        } else {
            response.setAssistantMessage("Aqui está o resumo analítico e consolidado para *" + disciplina + "* (" + assunto + "):");
            response.setBulletPoints(List.of(
                    "Conceitos Fundamentais: Definições axiomáticas e pré-requisitos essenciais da disciplina",
                    "Equações / Estruturas Chave: Aplicação das regras canônicas na resolução de problemas",
                    "Pegadinhas Frequentes: Erros de sinal, condições de contorno e casos limites nos exames",
                    "Relação Interdisciplinar: Como este módulo se conecta com as próximas matérias do curso"
            ));
        }

        response.setQuickOptions(List.of(
                new QuickOptionDTO("chip_exercicio", "📐 Explicar Exercício Prático", "EXPLICAR_EXERCICIO", "Aplicar conceitos"),
                new QuickOptionDTO("chip_dicas", "💡 Dicas de Prova", "DICAS_PROVA", "Técnicas de memorização"),
                new QuickOptionDTO("chip_professores", "👨‍🏫 Ver Professores Desta Matéria", "VER_PROFESSORES", "Agendamento de mentoria")
        ));

        return response;
    }

    /**
     * Resposta determinística para o chip 'Plano de Estudo'.
     */
    private TriageResponseDTO buildPlanoEstudosResponse(String disciplina, String assunto) {
        TriageResponseDTO response = new TriageResponseDTO();
        response.setNextStep(TriageStep.RECOMMENDATION);
        response.setAssistantMessage("Plano Semanal de Estudos estruturado para *" + disciplina + "* com base em neurociência educacional:");

        response.setBulletPoints(List.of(
                "Segunda & Quarta: Bloco Teórico de 50 minutos de foco ativo + 10 minutos de pausa reflexiva + 30 minutos de resolução de lista",
                "Terça & Quinta: Resolução prática de exercícios de nível intermediário e depuração guiada de erros",
                "Sexta-feira: Repetição Espaçada (Curva de Ebbinghaus) focando nos pontos com maior índice de dúvida",
                "Sábado de Manhã: Simulado prático cronometrado para adaptação ao ritmo de prova presencial",
                "Domingo: Descanso neurológico para consolidação de memória de longo prazo"
        ));

        response.setQuickOptions(List.of(
                new QuickOptionDTO("chip_dicas_estudo", "💡 Dicas de Memorização", "DICAS_PROVA", "Técnica de Feynman e Active Recall"),
                new QuickOptionDTO("chip_resumo_semanal", "📝 Resumo de Conteúdo", "GERAR_RESUMO", "Material de apoio"),
                new QuickOptionDTO("chip_agendar_semana", "📅 Agendar Mentoria Semanal", "AGENDAR_MENTORIA", "Acompanhamento docente")
        ));

        return response;
    }

    /**
     * Resposta determinística para o chip 'Dicas de Prova'.
     */
    private TriageResponseDTO buildDicasProvaResponse(String disciplina, String assunto) {
        TriageResponseDTO response = new TriageResponseDTO();
        response.setNextStep(TriageStep.RECOMMENDATION);
        response.setAssistantMessage("Técnicas de alto rendimento para revisões e avaliações de *" + disciplina + "*:");

        response.setBulletPoints(List.of(
                "Prática Ativa de Recordação (Active Recall): Feche os apontamentos e tente demonstrar a dedução ou algoritmo em folha em branco",
                "Técnica de Feynman: Ensine o conteúdo em voz alta com vocabulário simples, identificando onde ocorrem travas conceituais",
                "Estratégia de Resolução no Exame: Comece pelas questões com raciocínio imediato para garantir nota base e aliviar a tensão",
                "Verificação de Condições de Contorno: Sempre teste casos extremos (nulos, valores negativos, vetores vazios ou limites infinitos)",
                "Sono e Hidratação: A consolidação de conexões sinápticas ocorre no estágio profundo do sono na véspera do exame"
        ));

        response.setQuickOptions(List.of(
                new QuickOptionDTO("chip_plano", "📅 Montar Plano Semanal", "PLANO_ESTUDO", "Organizar rotina"),
                new QuickOptionDTO("chip_exercicio_prova", "📐 Explicar Questão Típica", "EXPLICAR_EXERCICIO", "Exercício comentado"),
                new QuickOptionDTO("chip_simulado", "👨‍🏫 Agendar Simulado com Professor", "AGENDAR_MENTORIA", "Sessão preparatória")
        ));

        return response;
    }

    /**
     * Resposta pedagógica geral contextualizada.
     */
    private TriageResponseDTO buildRespostaGeralContextualizada(String disciplina, String assunto, String textoOriginal) {
        TriageResponseDTO response = new TriageResponseDTO();
        response.setNextStep(TriageStep.RECOMMENDATION);
        response.setAssistantMessage("Compreendi sua consulta sobre *" + disciplina + "*! Este tema é recorrente nas avaliações do semestre universitário. Como posso te orientar com máxima precisão?");

        response.setBulletPoints(List.of(
                "Orientação Conceitual: Estruturação dos fundamentos e fórmulas da disciplina",
                "Exercícios Resolvidos: Passo a passo demonstrativo com aplicação prática",
                "Conexão com Mentores: Disponibilidade de professores especializados na plataforma para atendimento individual"
        ));

        response.setQuickOptions(List.of(
                new QuickOptionDTO("chip_explicar", "📐 Explicar Exercício", "EXPLICAR_EXERCICIO", "Resolução passo a passo"),
                new QuickOptionDTO("chip_resumo", "📝 Gerar Resumo", "GERAR_RESUMO", "Pontos-chave do conteúdo"),
                new QuickOptionDTO("chip_plano", "📅 Plano de Estudos", "PLANO_ESTUDO", "Cronograma de aprendizado"),
                new QuickOptionDTO("chip_dicas", "💡 Dicas de Prova", "DICAS_PROVA", "Técnicas de resolução")
        ));

        return response;
    }

    /**
     * Passo 1: Boas-vindas e apresentação das disciplinas disponíveis
     */
    private TriageResponseDTO handleInitialStep() {
        TriageResponseDTO response = TriageResponseDTO.create(
                "Olá! Sou o Assistente Acadêmico do Orienta 2.0 🎓 Fui desenvolvido com regras determinísticas para ajudá-lo a tirar dúvidas, revisar exercícios e encontrar professores qualificados com custo zero e total transparência. Qual disciplina você precisa de reforço?",
                TriageStep.SUBJECT_SELECTION,
                "RULE_TREE_INITIAL_V9"
        );

        List<QuickOptionDTO> options = CATALOGO_DISCIPLINAS.keySet().stream()
                .map(disc -> new QuickOptionDTO(
                        "opt_" + disc.toLowerCase().replaceAll("[^a-z0-9]", "_"),
                        disc,
                        disc,
                        "Ver tópicos de " + disc
                ))
                .collect(Collectors.toList());

        options.add(new QuickOptionDTO("opt_nao_encontrei", "Outra matéria (Falar com Suporte)", "OUTRA_DISCIPLINA", "Encaminhar para fila de suporte"));

        response.setQuickOptions(options);
        return response;
    }

    /**
     * Passo 2: Seleção do Assunto dentro da disciplina selecionada
     */
    private TriageResponseDTO handleSubjectSelection(TriageRequestDTO request) {
        String disciplina = request.getSelectedOptionPayload() != null ? request.getSelectedOptionPayload() : request.getDisciplinaNome();

        if ("OUTRA_DISCIPLINA".equalsIgnoreCase(disciplina)) {
            return buildSupportHandoffResponse("Aluno procura disciplina que não consta no catálogo padrão");
        }

        if (disciplina == null || !CATALOGO_DISCIPLINAS.containsKey(disciplina)) {
            TriageResponseDTO retry = handleInitialStep();
            retry.setAssistantMessage("Não identifiquei a disciplina selecionada no nosso catálogo. Por favor, selecione uma das opções abaixo:");
            return retry;
        }

        List<String> topicos = CATALOGO_DISCIPLINAS.get(disciplina);
        TriageResponseDTO response = TriageResponseDTO.create(
                "Ótimo! Para a disciplina de *" + disciplina + "*, qual destes tópicos é o foco da sua dúvida?",
                TriageStep.TOPIC_SELECTION,
                "RULE_TREE_TOPIC_V9"
        );

        List<QuickOptionDTO> options = topicos.stream()
                .map(topico -> new QuickOptionDTO(
                        "top_" + topico.toLowerCase().replaceAll("[^a-z0-9]", "_"),
                        topico,
                        topico,
                        "Foco em " + topico
                ))
                .collect(Collectors.toList());

        options.add(new QuickOptionDTO("top_geral", "Revisão Geral da Disciplina", "Revisão Geral", "Abrange todos os tópicos"));

        response.setQuickOptions(options);
        return response;
    }

    /**
     * Passo 3: Escolha de modalidade e orçamento
     */
    private TriageResponseDTO handleTopicSelection(TriageRequestDTO request) {
        String assunto = request.getSelectedOptionPayload() != null ? request.getSelectedOptionPayload() : request.getAssuntoNome();

        TriageResponseDTO response = TriageResponseDTO.create(
                "Perfeito! Assunto definido: *" + assunto + "*. Agora selecione a modalidade preferida e faixa de valor hora/aula que você busca:",
                TriageStep.MODALITY_BUDGET,
                "RULE_TREE_MODALITY_V9"
        );

        List<QuickOptionDTO> options = List.of(
                new QuickOptionDTO("mod_online_padrao", "Online (Até R$ 50/h)", "ONLINE:50.00", "Aulas remotas econômicas"),
                new QuickOptionDTO("mod_online_flex", "Online (Até R$ 75/h)", "ONLINE:75.00", "Aulas remotas com mais opções"),
                new QuickOptionDTO("mod_presencial", "Presencial (Até R$ 75/h)", "PRESENCIAL:75.00", "Aulas presenciais na sua região"),
                new QuickOptionDTO("mod_qualquer", "Tanto faz (Qualquer Modalidade)", "AMBAS:100.00", "Maior disponibilidade")
        );

        response.setQuickOptions(options);
        return response;
    }

    /**
     * Passo 4: Filtragem e recomendação de professores
     */
    private TriageResponseDTO handleModalityAndBudget(TriageRequestDTO request) {
        String payload = request.getSelectedOptionPayload();
        String modalidade = "AMBAS";
        BigDecimal orcamentoMax = new BigDecimal("80.00");

        if (payload != null && payload.contains(":")) {
            String[] parts = payload.split(":");
            modalidade = parts[0];
            orcamentoMax = new BigDecimal(parts[1]);
        } else if (request.getModalidade() != null && request.getOrcamentoMaximo() != null) {
            modalidade = request.getModalidade();
            orcamentoMax = request.getOrcamentoMaximo();
        }

        final String finalMod = modalidade;
        final BigDecimal finalOrcamento = orcamentoMax;
        final String disciplinaBuscada = request.getDisciplinaNome() != null ? request.getDisciplinaNome() : "Cálculo I";

        List<ProfessorResumoDTO> compativeis = buscarProfessoresRecomendados(disciplinaBuscada, finalMod, finalOrcamento);

        if (compativeis.isEmpty()) {
            TriageResponseDTO noResult = TriageResponseDTO.create(
                    "Não encontramos nenhum professor disponível exatamente com esses critérios (Disciplina: " + disciplinaBuscada +
                            ", Modalidade: " + finalMod + ", Limite: R$ " + finalOrcamento + "). Deseja ampliar a busca ou abrir chamado no Suporte?",
                    TriageStep.SUPPORT_HANDOFF,
                    "RULE_TREE_NO_MATCH_V9"
            );

            noResult.setQuickOptions(List.of(
                    new QuickOptionDTO("opt_suporte", "Encaminhar ao Suporte Humano", "ABRIR_CHAMADO", "Atendimento administrativo"),
                    new QuickOptionDTO("opt_recomecar", "Reiniciar Triagem", "REINICIAR", "Tentar outros filtros")
            ));
            return noResult;
        }

        TriageResponseDTO response = TriageResponseDTO.create(
                "Encontrei " + compativeis.size() + " professor(es) disponível(is) para *" + disciplinaBuscada +
                        "* dentro do seu orçamento máximo de R$ " + finalOrcamento + ". Veja a lista e selecione para revisar:",
                TriageStep.RECOMMENDATION,
                "RULE_TREE_RECOMMENDATION_V9"
        );

        response.setRecommendedTeachers(compativeis);

        // Prepara o primeiro professor como card de resumo inicial
        ProfessorResumoDTO topProfessor = compativeis.get(0);
        ActionCardDTO actionCard = new ActionCardDTO(
                topProfessor.getId(),
                topProfessor.getNome(),
                disciplinaBuscada,
                request.getAssuntoNome() != null ? request.getAssuntoNome() : "Dúvidas Gerais",
                finalMod,
                topProfessor.getValorHora(),
                "/agendamento/" + topProfessor.getId()
        );
        response.setActionCard(actionCard);

        List<QuickOptionDTO> options = compativeis.stream()
                .map(p -> new QuickOptionDTO(
                        "prof_" + p.getId(),
                        p.getNome() + " (R$ " + p.getValorHora() + "/h)",
                        String.valueOf(p.getId()),
                        p.getTituloAcademico() + " • ⭐ " + p.getAvaliacaoMedia()
                ))
                .collect(Collectors.toList());

        response.setQuickOptions(options);
        return response;
    }

    /**
     * Passo 5: Atualiza o card de resumo com o professor selecionado
     */
    private TriageResponseDTO handleRecommendationFinalization(TriageRequestDTO request) {
        String profIdStr = request.getSelectedOptionPayload();
        Long profId = profIdStr != null && profIdStr.matches("\\d+") ? Long.parseLong(profIdStr) : 101L;

        List<ProfessorResumoDTO> catalogo = buscarProfessoresRecomendados(request.getDisciplinaNome(), "AMBAS", null);
        ProfessorResumoDTO professor = catalogo.stream()
                .filter(p -> p.getId().equals(profId))
                .findFirst()
                .orElse(catalogo.isEmpty() ? PROFESSORES_CATALOGO.get(0) : catalogo.get(0));

        TriageResponseDTO response = TriageResponseDTO.create(
                "Você selecionou " + professor.getNome() + ". Abaixo está o Card de Ação consolidado. Lembre-se: o assistente NÃO realiza o agendamento automaticamente por segurança (F9-T7). Clique no botão abaixo para escolher o horário oficial.",
                TriageStep.COMPLETED,
                "RULE_TREE_ACTION_CARD_V9"
        );

        ActionCardDTO actionCard = new ActionCardDTO(
                professor.getId(),
                professor.getNome(),
                request.getDisciplinaNome() != null ? request.getDisciplinaNome() : "Disciplina Selecionada",
                request.getAssuntoNome() != null ? request.getAssuntoNome() : "Tópicos de Estudo",
                request.getModalidade() != null ? request.getModalidade() : "ONLINE",
                professor.getValorHora(),
                "/agendamento/" + professor.getId()
        );

        response.setActionCard(actionCard);
        response.setQuickOptions(List.of(
                new QuickOptionDTO("act_agendar", "Ir para Confirmação de Agendamento", "NAVIGATE_BOOKING", "Abre a tela oficial"),
                new QuickOptionDTO("act_outra", "Buscar Outro Professor", "REINICIAR", "Voltar ao início")
        ));

        return response;
    }

    /**
     * Busca professores cadastrados no banco ou catálogo demonstrativo de contingência.
     */
    public List<ProfessorResumoDTO> buscarProfessoresRecomendados(String disciplinaNome, String modalidade, BigDecimal precoMax) {
        String disc = disciplinaNome != null ? disciplinaNome : "Cálculo I";
        String modNorm = (modalidade != null && !modalidade.isBlank()) ? modalidade.toUpperCase() : "AMBAS";
        BigDecimal limite = precoMax != null ? precoMax : new BigDecimal("9999.00");

        // 1. Tenta buscar no banco real se os repositórios estiverem presentes
        if (usuarioRepository != null && saberRepository != null) {
            try {
                var saberOpt = saberRepository.findByNome(disc);
                if (saberOpt.isPresent()) {
                    Long saberId = saberOpt.get().getId();
                    List<Usuario> docentes = usuarioRepository.findProfessoresPorMateriasOrdenadosPorNota(List.of(saberId));
                    if (docentes != null && !docentes.isEmpty()) {
                        return docentes.stream()
                                .map(u -> new ProfessorResumoDTO(
                                        u.getId(),
                                        u.getNome(),
                                        "Docente Certificado Orienta",
                                        BigDecimal.valueOf(u.getPrecoHora() != null ? u.getPrecoHora() : 50.00),
                                        u.getNotaMedia() != null ? u.getNotaMedia() : 5.0,
                                        u.getTotalAvaliacoes() != null ? u.getTotalAvaliacoes() : 1,
                                        Boolean.TRUE.equals(u.getOferecePresencial()) ? List.of("ONLINE", "PRESENCIAL") : List.of("ONLINE"),
                                        u.getBairro() != null ? u.getBairro() : "Centro",
                                        List.of(disc),
                                        true
                                ))
                                .filter(p -> p.getValorHora().compareTo(limite) <= 0)
                                .filter(p -> "AMBAS".equals(modNorm) || p.getModalidades().contains(modNorm))
                                .collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                log.warn("Falha ao buscar professores no banco de dados. Recorrendo ao catálogo em memória: {}", e.getMessage());
            }
        }

        // 2. Fallback determinístico para catálogo estático
        return PROFESSORES_CATALOGO.stream()
                .filter(p -> p.getDisciplinas().contains(disc) || disc.equalsIgnoreCase("TODAS"))
                .filter(p -> p.getValorHora().compareTo(limite) <= 0)
                .filter(p -> "AMBAS".equals(modNorm) || p.getModalidades().contains(modNorm))
                .sorted(Comparator.comparing(ProfessorResumoDTO::getAvaliacaoMedia).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Encaminha mensagens incompreendidas ou fora do escopo para o suporte humano (F9-T5).
     */
    public TriageResponseDTO buildSupportHandoffResponse(String query) {
        String protocolo = "SUP-" + System.currentTimeMillis() % 1000000;
        log.warn("Mensagem fora do escopo de triagem direcionada para suporte: [{}]. Protocolo: {}", query, protocolo);

        TriageResponseDTO response = TriageResponseDTO.create(
                "Não consegui processar sua mensagem pelas regras determinísticas de triagem de aulas (" + (query != null ? query : "sem texto") +
                        "). Por segurança e para não gerar respostas incorretas, criei um rascunho de ticket na nossa fila de suporte humano.",
                TriageStep.SUPPORT_HANDOFF,
                "RULE_TREE_SUPPORT_FALLBACK_V9"
        );

        response.setSupportTicketDraftProtocol(protocolo);
        response.setQuickOptions(List.of(
                new QuickOptionDTO("sup_confirm", "Enviar para Suporte (Ticket #" + protocolo + ")", protocolo, "Abrir chamado formal"),
                new QuickOptionDTO("sup_restart", "Voltar à Triagem de Professores", "REINICIAR", "Retornar ao menu")
        ));

        return response;
    }

    /**
     * Identifica a disciplina a partir de menções explícitas ou termos-chave no texto.
     */
    private String identificarDisciplina(String disciplinaInformada, String texto) {
        if (disciplinaInformada != null && !disciplinaInformada.isBlank()) {
            return disciplinaInformada;
        }

        String lower = texto.toLowerCase();
        if (lower.contains("calculo") || lower.contains("cálculo") || lower.contains("integral") || lower.contains("derivada") || lower.contains("limite")) {
            return "Cálculo I";
        }
        if (lower.contains("arvore") || lower.contains("árvore") || lower.contains("lista") || lower.contains("algoritmo") || lower.contains("dados")) {
            return "Estrutura de Dados";
        }
        if (lower.contains("banco") || lower.contains("sql") || lower.contains("relacional") || lower.contains("acid") || lower.contains("normaliza")) {
            return "Banco de Dados";
        }
        if (lower.contains("objeto") || lower.contains("heranca") || lower.contains("polimorfismo") || lower.contains("classe") || lower.contains("java")) {
            return "Programação Orientada a Objetos";
        }
        if (lower.contains("rede") || lower.contains("tcp") || lower.contains("ip") || lower.contains("osi") || lower.contains("dns")) {
            return "Redes de Computadores";
        }

        return "Cálculo I";
    }

    /**
     * Normaliza a intenção pedagógica a partir do chip enviado ou palavras-chave na mensagem.
     */
    private String normalizarChipTipo(String chipEnviado, String texto) {
        if (chipEnviado != null && !chipEnviado.isBlank()) {
            String c = chipEnviado.trim().toUpperCase().replace("-", "_").replace(" ", "_");
            if (c.contains("EXERCICIO") || c.contains("EXPLICAR")) return "EXPLICAR_EXERCICIO";
            if (c.contains("RESUMO")) return "GERAR_RESUMO";
            if (c.contains("PLANO") || c.contains("ESTUDO")) return "PLANO_ESTUDO";
            if (c.contains("DICA") || c.contains("PROVA")) return "DICAS_PROVA";
            return c;
        }

        String lower = texto.toLowerCase();
        if (lower.contains("exercício") || lower.contains("exercicio") || lower.contains("resolver") || lower.contains("passo a passo")) {
            return "EXPLICAR_EXERCICIO";
        }
        if (lower.contains("resumo") || lower.contains("sintese") || lower.contains("síntese") || lower.contains("conceito")) {
            return "GERAR_RESUMO";
        }
        if (lower.contains("plano") || lower.contains("cronograma") || lower.contains("semanal") || lower.contains("rotina")) {
            return "PLANO_ESTUDO";
        }
        if (lower.contains("dica") || lower.contains("prova") || lower.contains("memorizacao") || lower.contains("memorização") || lower.contains("feynman")) {
            return "DICAS_PROVA";
        }

        return "GERAL";
    }

    private boolean isPedagogicChipQuery(String msg) {
        if (msg == null || msg.isBlank()) return false;
        String lower = msg.toLowerCase();
        return lower.contains("exercício") || lower.contains("exercicio") || lower.contains("resumo")
                || lower.contains("plano de estudo") || lower.contains("plano de estudos")
                || lower.contains("dica de estudo") || lower.contains("dicas de prova");
    }

    /**
     * Detecta se a mensagem é fora do escopo ou contém comandos que desrespeitam regras determinísticas.
     */
    public boolean isOutOfScopeQuery(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("pizza") || lower.contains("comprar") ||
                lower.contains("cancelar minha aula sozinho") ||
                lower.contains("debite meu cartao") ||
                lower.contains("fazer fofoca") ||
                lower.contains("quem ganhou o jogo");
    }
}
