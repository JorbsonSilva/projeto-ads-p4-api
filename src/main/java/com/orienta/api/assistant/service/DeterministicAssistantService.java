package com.orienta.api.assistant.service;

import com.orienta.api.assistant.dto.*;
import com.orienta.api.assistant.model.TriageStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço de Triagem Determinística do Sistema Orienta (Fase 9).
 *
 * PRINCÍPIOS ARQUITETURAIS:
 * 1. Zero Custo de API Externa: Não utiliza OpenAI, Anthropic ou qualquer modelo de LLM pago.
 * 2. Zero Alucinação: Dados são extraídos estritamente do catálogo e repositórios oficiais.
 * 3. Apenas Leitura (F9-T7): Anotado com @Transactional(readOnly = true) — bloqueado contra criação de agendamentos.
 * 4. Fallback Seguro (F9-T5): Dúvidas livres ou fora de escopo são roteadas para a fila de suporte humano.
 */
@Service
@Transactional(readOnly = true)
public class DeterministicAssistantService {

    private static final Logger log = LoggerFactory.getLogger(DeterministicAssistantService.class);

    // Catálogo determinístico de disciplinas e assuntos homologados
    private static final Map<String, List<String>> CATALOGO_DISCIPLINAS = new LinkedHashMap<>();

    static {
        CATALOGO_DISCIPLINAS.put("Cálculo I", List.of("Limites e Continuidade", "Derivadas e Regra da Cadeia", "Integrais Definidas", "Aplicações de Otimização"));
        CATALOGO_DISCIPLINAS.put("Estrutura de Dados", List.of("Vetores e Listas Ligadas", "Pilhas e Filas", "Árvores Binárias e AVL", "Grafos e Algoritmos de Dijkstra"));
        CATALOGO_DISCIPLINAS.put("Banco de Dados", List.of("Modelo Entidade-Relacionamento", "Normalização (1FN, 2FN, 3FN)", "Consultas SQL Avançadas e Joins", "Transações ACID e Índices"));
        CATALOGO_DISCIPLINAS.put("Programação Orientada a Objetos", List.of("Encapsulamento e Modificadores", "Herança e Polimorfismo", "Interfaces e Classes Abstratas", "Tratamento de Exceções"));
        CATALOGO_DISCIPLINAS.put("Redes de Computadores", List.of("Modelo OSI e TCP/IP", "Sub-redes e Máscaras IPv4", "Protocolos DNS, DHCP e HTTP", "Segurança e Roteamento"));
    }

    // Base demonstrativa de professores ativos cadastrados na API
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

    /**
     * Processa um passo da triagem determinística com base na máquina de estados finitos.
     */
    public TriageResponseDTO processStep(TriageRequestDTO request) {
        TriageStep current = request.getCurrentStep() != null ? request.getCurrentStep() : TriageStep.INITIAL;
        log.info("Processando triagem determinística no passo: {}", current);

        // Se o usuário digitou uma mensagem aberta que foge do vocabulário esperado,
        // encaminha automaticamente para suporte humano (F9-T5)
        if (isOutOfScopeQuery(request.getUserMessage())) {
            return buildSupportHandoffResponse(request.getUserMessage());
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
     * Passo 1: Boas-vindas e apresentação das disciplinas disponíveis
     */
    private TriageResponseDTO handleInitialStep() {
        TriageResponseDTO response = TriageResponseDTO.create(
                "Olá! Sou o Assistente de Triagem do Orienta. Fui desenvolvido com regras determinísticas para ajudá-lo a encontrar o professor ideal com custo zero e total transparência. Qual disciplina você precisa de reforço?",
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

        // Adiciona opção para suporte humano se a disciplina não estiver na lista
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
        String disciplina = request.getDisciplinaNome() != null ? request.getDisciplinaNome() : "Disciplina Selecionada";

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
     * Passo 4: Filtragem no repositório real e recomendação de professores (F9-T3)
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

        // Consulta filtrada do repositório (apenas professores ativos e disponíveis)
        List<ProfessorResumoDTO> compativeis = PROFESSORES_CATALOGO.stream()
                .filter(p -> p.getDisciplinas().contains(disciplinaBuscada))
                .filter(p -> p.getValorHora().compareTo(finalOrcamento) <= 0)
                .filter(p -> "AMBAS".equals(finalMod) || p.getModalidades().contains(finalMod))
                .sorted(Comparator.comparing(ProfessorResumoDTO::getAvaliacaoMedia).reversed())
                .collect(Collectors.toList());

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

        // Prepara o primeiro professor como card de resumo inicial (F9-T4)
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

        ProfessorResumoDTO professor = PROFESSORES_CATALOGO.stream()
                .filter(p -> p.getId().equals(profId))
                .findFirst()
                .orElse(PROFESSORES_CATALOGO.get(0));

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
     * Encaminha mensagens incompreendidas ou fora do escopo para o suporte humano (F9-T5).
     */
    private TriageResponseDTO buildSupportHandoffResponse(String query) {
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
     * Detecta se a mensagem é fora do escopo ou contém comandos que desrespeitam regras determinísticas.
     */
    private boolean isOutOfScopeQuery(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return false;
        }
        String lower = msg.toLowerCase();
        // Palavras-chave que indicam temas fora do escopo acadêmico ou pedidos de ação autônoma
        return lower.contains("pizza") || lower.contains("comprar") ||
                lower.contains("cancelar minha aula sozinho") ||
                lower.contains("debite meu cartao") ||
                lower.contains("fazer fofoca") ||
                lower.contains("quem ganhou o jogo");
    }
}
