package com.orienta.api.assistant.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.orienta.api.assistant.dto.PerguntaAssistenteDTO;
import com.orienta.api.assistant.dto.TriageRequestDTO;
import com.orienta.api.assistant.dto.TriageResponseDTO;
import com.orienta.api.assistant.service.DeterministicAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para o Assistente Acadêmico IA com Triagem e Chips (Fase 7 - Tarefa [BACK-018]).
 *
 * GARANTIAS DE SEGURANÇA E ARQUITETURA:
 * - Autenticação obrigatória via JWT Bearer Token associando a consulta ao usuário ativo.
 * - Motor de contingência determinístico resiliente (zero custo, zero dependência externa de LLMs pagos).
 * - Sem poderes de escrita direta em entidades de negócio como tb_aulas/tb_agendamentos (Apenas Leitura).
 * - Retorno no envelope padronizado ApiResponse<T>.
 */
@RestController
@RequestMapping({"/api/v1/assistente", "/api/v1/assistant", "/api/assistente", "/api/assistant"})
@Tag(name = "Assistente Acadêmico IA (v1)", description = "Triagem pedagógica determinística por regras e chips com contingência offline")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class AssistantController {

    private final DeterministicAssistantService assistantService;

    public AssistantController(DeterministicAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Endpoint de triagem pedagógica determinística com suporte a chips rápidos.
     */
    @Operation(summary = "Processar passo de triagem", description = "Executa transição na máquina de estados ou processa chips rápidos com respostas pedagógicas estruturadas.")
    @PostMapping("/triagem")
    public ResponseEntity<ApiResponse<TriageResponseDTO>> processTriageStep(@Valid @RequestBody TriageRequestDTO request) {
        Long usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        TriageResponseDTO response = assistantService.processStep(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Passo de triagem processado com sucesso"));
    }

    /**
     * Endpoint de processamento de perguntas livres contextualizadas com disciplinas/saberes acadêmicos.
     * Rastreabilidade: Tarefa [BACK-018]
     */
    @Operation(summary = "Perguntar ao assistente acadêmico", description = "Processa dúvidas acadêmicas livres ou acionamento de chips pedagógicos com templates determinísticos.")
    @PostMapping({"/perguntar", "/ask"})
    public ResponseEntity<ApiResponse<TriageResponseDTO>> perguntarAssistente(@Valid @RequestBody PerguntaAssistenteDTO request) {
        Long usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        if (request.getUsuarioId() == null && usuarioId != null) {
            request.setUsuarioId(usuarioId);
        }
        TriageResponseDTO response = assistantService.processarPergunta(request, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Orientação pedagógica formulada com sucesso"));
    }

    /**
     * Retorna a especificação transparente da árvore de decisão para auditoria da banca avaliadora.
     */
    @Operation(summary = "Consultar regras de decisão", description = "Retorna a especificação transparente do motor determinístico para fins de auditoria e contingência.")
    @GetMapping("/regras")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDecisionTreeRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("sistema", "Orienta - Assistente Determinístico Fase 9");
        rules.put("iaModelo", "NENHUM (Rule Engine Local Baseado em Autômato de Estados)");
        rules.put("custoApi", "R$ 0,00");
        rules.put("riscoAlucinacao", "0% (Respostas mapeadas estritamente no catálogo)");
        rules.put("chipsPedagogicos", List.of(
                "EXPLICAR_EXERCICIO ➔ Resolução estruturada com fórmulas LIATE / Algoritmos e passos",
                "GERAR_RESUMO ➔ Síntese analítica com propriedades invariantes e complexidades O(log n)",
                "PLANO_ESTUDO ➔ Cronograma semanal equilibrado fundamentado em neurociência educacional",
                "DICAS_PROVA ➔ Metodologias de alta retenção (Active Recall e Técnica de Feynman)"
        ));
        rules.put("etapas", List.of(
                "INITIAL ➔ Boas-vindas e seleção de disciplina",
                "SUBJECT_SELECTION ➔ Validação da disciplina e oferta de tópicos",
                "TOPIC_SELECTION ➔ Validação do tópico e oferta de modalidade/orçamento",
                "MODALITY_BUDGET ➔ Consulta filtrada de professores reais",
                "RECOMMENDATION ➔ Apresentação do ranking de professores e Card de Ação",
                "SUPPORT_HANDOFF ➔ Rascunho de ticket para dúvidas incompreendidas",
                "COMPLETED ➔ Exigência de confirmação manual pelo aluno"
        ));
        rules.put("politicaSeguranca", "Bloqueio estrito de mutações de banco a partir de mensagens do assistente (F9-T7)");

        return ResponseEntity.ok(ApiResponse.ok(rules, "Regras de decisão do assistente recuperadas"));
    }

    /**
     * FAQ determinístico embutido sem chamada externa.
     */
    @Operation(summary = "Consultar FAQ acadêmico", description = "Respostas prontas para as dúvidas mais frequentes sobre o assistente.")
    @GetMapping("/faq")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getFAQ() {
        List<Map<String, String>> faq = List.of(
                Map.of("pergunta", "O assistente pode agendar uma aula sozinho?",
                       "resposta", "Não. O assistente apenas sugere professores disponíveis no catálogo. O agendamento requer confirmação manual explícita do aluno."),
                Map.of("pergunta", "Existe custo por mensagem enviada?",
                       "resposta", "Zero custo. O assistente funciona localmente por regras determinísticas compiladas na API Spring Boot."),
                Map.of("pergunta", "O que acontece se eu fizer uma pergunta fora do escopo acadêmico?",
                       "resposta", "O sistema detecta que a mensagem está fora do catálogo e direciona para a fila de suporte humano com protocolo SUP-.")
        );
        return ResponseEntity.ok(ApiResponse.ok(faq, "FAQ recuperado com sucesso"));
    }
}
