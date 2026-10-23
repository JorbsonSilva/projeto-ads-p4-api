package com.orienta.api.assistant.controller;

import com.orienta.api.assistant.dto.TriageRequestDTO;
import com.orienta.api.assistant.dto.TriageResponseDTO;
import com.orienta.api.assistant.service.DeterministicAssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para o Assistente Controlado por Regras (Fase 9).
 *
 * GARANTIAS DE SEGURANÇA:
 * - Totalmente desacoplado de chaves e provedores externos de IA (zero custo, zero dependência externa).
 * - Sem endpoints com poderes de escrita direta em entidades de negócio como tb_agendamentos.
 * - Toda a árvore de decisão é auditável e pública.
 */
@RestController
@RequestMapping("/api/v1/assistente")
@CrossOrigin(origins = "*")
public class AssistantController {

    private final DeterministicAssistantService assistantService;

    public AssistantController(DeterministicAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Endpoint central de processamento determinístico da triagem.
     */
    @PostMapping("/triagem")
    public ResponseEntity<TriageResponseDTO> processTriageStep(@Valid @RequestBody TriageRequestDTO request) {
        TriageResponseDTO response = assistantService.processStep(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna a especificação transparente da árvore de decisão para auditoria da banca avaliadora.
     */
    @GetMapping("/regras")
    public ResponseEntity<Map<String, Object>> getDecisionTreeRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("sistema", "Orienta - Assistente Determinístico Fase 9");
        rules.put("iaModelo", "NENHUM (Rule Engine Local Baseado em Autômato de Estados)");
        rules.put("custoApi", "R$ 0,00");
        rules.put("riscoAlucinacao", "0% (Respostas mapeadas estritamente no catálogo)");
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

        return ResponseEntity.ok(rules);
    }

    /**
     * FAQ determinístico embutido sem chamada externa.
     */
    @GetMapping("/faq")
    public ResponseEntity<List<Map<String, String>>> getFAQ() {
        List<Map<String, String>> faq = List.of(
                Map.of("pergunta", "O assistente pode agendar uma aula sozinho?",
                       "resposta", "Não. O assistente apenas sugere professores disponíveis no catálogo. O agendamento requer confirmação manual explícita do aluno."),
                Map.of("pergunta", "Existe custo por mensagem enviada?",
                       "resposta", "Zero custo. O assistente funciona localmente por regras determinísticas compiladas na API Spring Boot."),
                Map.of("pergunta", "O que acontece se eu fizer uma pergunta fora do escopo acadêmico?",
                       "resposta", "O sistema detecta que a mensagem está fora do catálogo e direciona para a fila de suporte humano.")
        );
        return ResponseEntity.ok(faq);
    }
}
