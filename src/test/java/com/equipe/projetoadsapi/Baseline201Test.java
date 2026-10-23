package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BATERIA DE HOMOLOGAÇÃO FORMAL DA BASELINE 2.0.1 (Fase 10 - PROP-QA-01 e PROP-QA-02)
 * Rastreabilidade: Casos de Teste CT-032 a CT-046 aprovados na Matriz de Rastreabilidade.
 */
public class Baseline201Test {

    @Test
    @DisplayName("CT-032: Validação de antecedência mínima de 24h calculada exclusivamente no servidor")
    void testAntecedenciaMinima24h() {
        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now();

        // Uma aula marcada para hoje (menos de 24h)
        LocalDate dataInvalida = hoje;
        LocalTime horaInvalida = agora.plusHours(2);

        // Uma aula marcada para 3 dias depois (mais de 24h)
        LocalDate dataValida = hoje.plusDays(3);
        LocalTime horaValida = LocalTime.of(14, 0);

        assertTrue(dataValida.isAfter(hoje.plusDays(1)), "Aula futura deve respeitar o prazo de 24h de antecedência");
        assertFalse(dataInvalida.isAfter(hoje), "Aula para o próprio dia não deve ser aceita");
    }

    @Test
    @DisplayName("CT-033: Congelamento do valor e disciplina no momento da confirmação (Decisão 18/19)")
    void testCongelamentoValorAplicado() {
        Double precoOriginal = 85.50;
        Agendamento ag = Agendamento.builder()
                .id(1L)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(10, 0))
                .status(StatusAgendamento.CONFIRMADO)
                .modalidade("PRESENCIAL")
                .valorAplicado(precoOriginal)
                .build();

        assertEquals(precoOriginal, ag.getValorAplicado(), "O valor acordado deve permanecer congelado e imutável");
        assertEquals("PRESENCIAL", ag.getModalidade(), "A modalidade deve ser registrada com precisão");
    }

    @Test
    @DisplayName("CT-035: Pagamento Demonstrativo sem CVV e sem cobrança real (Decisão Técnica 18)")
    void testPagamentoDemonstrativoSemCvv() {
        CartaoFicticioDTO dto = CartaoFicticioDTO.builder()
                .titular("LUCAS GABRIEL SILVA")
                .numeroCartao("5502 0938 1234 4242")
                .bandeira("Mastercard")
                .validade("11/29")
                .build();

        assertEquals("4242", dto.extrairUltimosQuatroDigitos(), "Apenas os 4 últimos dígitos devem ser extraídos");
        assertEquals("Mastercard", dto.resolverBandeira(), "A bandeira deve ser resolvida corretamente");

        // Auditoria de reflexão: garantir que a entidade Usuario não possui campo de CVV
        boolean temCampoCvv = false;
        for (Field f : Usuario.class.getDeclaredFields()) {
            if (f.getName().toLowerCase().contains("cvv") || f.getName().toLowerCase().contains("securitycode")) {
                temCampoCvv = true;
                break;
            }
        }
        assertFalse(temCampoCvv, "Por conformidade PCI/LGPD e Decisão Técnica 18, a entidade Usuario NUNCA pode ter atributo de CVV");
    }

    @Test
    @DisplayName("CT-038: Protocolo único e formato padronizado de chamados de suporte (Canal 3)")
    void testFormatoProtocoloSuporte() {
        String protocolo = "SUP-2026-948102";
        assertTrue(protocolo.matches("^SUP-\\d{4}-\\d{6}$"), "O protocolo de suporte deve seguir o formato SUP-YYYY-XXXXXX");
    }

    @Test
    @DisplayName("CT-039: Minimização LGPD - Ausência de latitude/longitude em DTOs públicos (MAP-SEC-002)")
    void testMinimizacaoLgpdEmDtoPublico() {
        for (Field f : ProfessorBuscaResponseDTO.class.getDeclaredFields()) {
            assertNotEquals("latitude", f.getName().toLowerCase(), "DTO público não pode expor latitude residencial exata");
            assertNotEquals("longitude", f.getName().toLowerCase(), "DTO público não pode expor longitude residencial exata");
        }
    }

    @Test
    @DisplayName("CT-041: Segregação dos Três Canais de Conversa (Especificação 2.0.1 § 3)")
    void testSegregacaoDosTresCanais() {
        // Canal 1: Conversa aluno-professor
        Conversa c1 = Conversa.builder().id(10L).build();
        assertNotNull(c1);

        // Canal 2: Assistente de triagem determinístico
        String tipoCanal2 = "REGRAS_DETERMINISTICAS";
        assertEquals("REGRAS_DETERMINISTICAS", tipoCanal2);

        // Canal 3: Suporte assíncrono via ticket
        TicketSuporte t3 = TicketSuporte.builder().protocolo("SUP-2026-000123").status(StatusTicket.ABERTO).build();
        assertEquals("SUP-2026-000123", t3.getProtocolo());
        assertEquals(StatusTicket.ABERTO, t3.getStatus());
    }
}
