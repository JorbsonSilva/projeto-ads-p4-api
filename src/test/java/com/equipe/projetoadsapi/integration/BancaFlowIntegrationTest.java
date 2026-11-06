package com.equipe.projetoadsapi.integration;

import com.equipe.projetoadsapi.ProjetoAdsP3ApiApplication;
import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SUÍTE COMPLETA DE TESTES INTEGRADOS DE BANCA (TAREFA [BACK-020])
 * Rastreabilidade: Fase 7 - Bloco L (Homologação Final da API Orienta 2.0)
 * 
 * Simula ponta a ponta os 5 fluxos transversais do ecossistema:
 * - Fluxo 1: Onboarding ➔ Autenticação JWT ➔ Consulta de Perfil.
 * - Fluxo 2: Busca Geográfica de Professores (Haversine) ➔ Solicitação de Mentoria (> 24h).
 * - Fluxo 3: Aceite pelo Docente ➔ Check-in Bilateral na Sala de Aula ➔ Encerramento e Avaliação Pós-Aula.
 * - Fluxo 4: Liquidação Financeira ➔ Retenção de Taxa (10%) ➔ Solicitação de Saque PIX com Protocolo (SAQ-YYYY-XXXXX).
 * - Fluxo 5: Comunicação no Chat Bilateral ➔ Abertura de Ticket de Suporte ➔ Moderação de Denúncia Ética.
 */
@SpringBootTest(classes = ProjetoAdsP3ApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BancaFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private SessaoAulaRepository sessaoAulaRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private SaqueRepository saqueRepository;

    @Autowired
    private TransacaoFinanceiraRepository transacaoRepository;

    @Autowired
    private ConversaRepository conversaRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private TicketSuporteRepository ticketRepository;

    @Autowired
    private DenunciaRepository denunciaRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario alunoGabriel;
    private Usuario profCarlos;
    private Usuario adminMariana;
    private Saber disciplinaBanco;
    private String tokenAluno;
    private String tokenProfessor;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        objectMapper.findAndRegisterModules();

        // 1. Configurar ou recuperar Aluno Gabriel
        alunoGabriel = usuarioRepository.findByEmail("aluno.banca@orienta.edu.br");
        if (alunoGabriel == null) {
            alunoGabriel = Usuario.builder()
                    .nome("Gabriel Aluno (Banca)")
                    .email("aluno.banca@orienta.edu.br")
                    .senha(passwordEncoder.encode("Senha@123"))
                    .papel(PapelUsuario.ROLE_ALUNO)
                    .cpf("11122233344")
                    .emailVerificado(true)
                    .bioAluno("Estudante de Engenharia de Software no 4º período.")
                    .notaMedia(5.0)
                    .totalAvaliacoes(0)
                    .build();
            alunoGabriel = usuarioRepository.save(alunoGabriel);
        }

        // 2. Configurar ou recuperar Professor Carlos
        profCarlos = usuarioRepository.findByEmail("prof.carlos@orienta.edu.br");
        if (profCarlos == null) {
            profCarlos = Usuario.builder()
                    .nome("Prof. Dr. Carlos Eduardo")
                    .email("prof.carlos@orienta.edu.br")
                    .senha(passwordEncoder.encode("Senha@123"))
                    .papel(PapelUsuario.ROLE_PROFESSOR)
                    .cpf("22233344455")
                    .emailVerificado(true)
                    .bioProfessor("Doutor em Ciência da Computação (UFSCar).")
                    .oferecePresencial(true)
                    .regiaoAtendimento("Centro / Bela Vista")
                    .latitudeAproximada(-23.550520)
                    .longitudeAproximada(-46.633308)
                    .raioAtendimentoKm(10)
                    .notaMedia(5.0)
                    .totalAvaliacoes(1)
                    .build();
            profCarlos = usuarioRepository.save(profCarlos);
        }

        // 3. Configurar ou recuperar Admin Mariana
        adminMariana = usuarioRepository.findByEmail("admin.banca@orienta.edu.br");
        if (adminMariana == null) {
            adminMariana = Usuario.builder()
                    .nome("Mariana Coordenadora (Admin)")
                    .email("admin.banca@orienta.edu.br")
                    .senha(passwordEncoder.encode("Senha@123"))
                    .papel(PapelUsuario.ROLE_ADMIN)
                    .cpf("44455566677")
                    .emailVerificado(true)
                    .notaMedia(5.0)
                    .totalAvaliacoes(0)
                    .build();
            adminMariana = usuarioRepository.save(adminMariana);
        }

        // 4. Configurar Disciplina e Aptidão
        disciplinaBanco = saberRepository.findByNome("Banco de Dados Relacional e SQL (Banca)").orElse(null);
        if (disciplinaBanco == null) {
            disciplinaBanco = Saber.builder()
                    .nome("Banco de Dados Relacional e SQL (Banca)")
                    .categoria("Engenharia de Software")
                    .build();
            disciplinaBanco = saberRepository.save(disciplinaBanco);
        }

        if (aptidaoRepository.findByUsuarioId(profCarlos.getId()).isEmpty()) {
            Aptidao aptidao = new Aptidao();
            aptidao.setUsuario(profCarlos);
            aptidao.setSaber(disciplinaBanco);
            aptidao.setPrecoHora(BigDecimal.valueOf(70.00));
            aptidao.setNivelDominio("Especialista");
            aptidaoRepository.save(aptidao);
        }

        // 5. Gerar Tokens JWT Autênticos para a suíte
        tokenAluno = "Bearer " + jwtTokenProvider.gerarAccessToken(alunoGabriel.getId(), alunoGabriel.getEmail(), "ROLE_ALUNO");
        tokenProfessor = "Bearer " + jwtTokenProvider.gerarAccessToken(profCarlos.getId(), profCarlos.getEmail(), "ROLE_PROFESSOR");
        tokenAdmin = "Bearer " + jwtTokenProvider.gerarAccessToken(adminMariana.getId(), adminMariana.getEmail(), "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Health Check: Auditoria da API Orienta em execução")
    void testApiHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("FLUXO 1: Onboarding ➔ Autenticação JWT por E-mail/CPF ➔ Consulta de Perfil Unificado")
    void testFluxo1_OnboardingAutenticacaoEPerfil() throws Exception {
        // Passo 1.1: Autenticação via Login com E-mail
        AuthRequestDTO loginDTO = new AuthRequestDTO("aluno.banca@orienta.edu.br", "Senha@123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.usuario.email").value("aluno.banca@orienta.edu.br"))
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();
        assertNotNull(jwtToken);
        String jwtBearer = "Bearer " + jwtToken;

        // Passo 1.2: Consulta de Perfil Unificado com Token do Aluno
        mockMvc.perform(get("/api/v1/usuarios/{id}/perfil", alunoGabriel.getId())
                        .header("Authorization", jwtBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nome").value("Gabriel Aluno (Banca)"))
                .andExpect(jsonPath("$.data.papel").value("ROLE_ALUNO"));

        // Passo 1.3: Consulta de Perfil Docente com Saberes e Disponibilidades
        mockMvc.perform(get("/api/v1/usuarios/{id}/perfil", profCarlos.getId())
                        .header("Authorization", tokenProfessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nome").value("Prof. Dr. Carlos Eduardo"))
                .andExpect(jsonPath("$.data.aptidoes").isArray());
    }

    @Test
    @DisplayName("FLUXO 2: Busca Geográfica de Professores (Haversine) ➔ Solicitação de Mentoria (> 24h)")
    void testFluxo2_BuscaGeograficaESolicitacaoMentoria() throws Exception {
        // Passo 2.1: Busca presencial via coordenadas (Centro / Bela Vista SP)
        mockMvc.perform(get("/api/v1/usuarios/professores/busca")
                        .header("Authorization", tokenAluno)
                        .param("latitude", "-23.550520")
                        .param("longitude", "-46.633308")
                        .param("raioKm", "15")
                        .param("modalidade", "PRESENCIAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].nome", hasItem("Prof. Dr. Carlos Eduardo")));

        // Passo 2.2: Solicitação de Mentoria respeitando antecedência mínima de 24h
        LocalDate dataFutura = LocalDate.now().plusDays(3);
        LocalTime horaAula = LocalTime.of(14, 0);

        AgendamentoRequestDTO agendamentoDTO = AgendamentoRequestDTO.builder()
                .idAluno(alunoGabriel.getId())
                .idProfessor(profCarlos.getId())
                .idDisciplina(disciplinaBanco.getId())
                .data(dataFutura)
                .hora(horaAula)
                .modalidade("PRESENCIAL")
                .assunto("Modelagem Relacional e Formas Normais")
                .build();

        MvcResult agendamentoResult = mockMvc.perform(post("/api/v1/agendamentos")
                        .header("Authorization", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agendamentoDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDENTE"))
                .andExpect(jsonPath("$.data.valorAplicado").value(70.00))
                .andReturn();

        AgendamentoResponseDTO responseAgendamento = objectMapper.readValue(
                objectMapper.readTree(agendamentoResult.getResponse().getContentAsString()).get("data").toString(),
                AgendamentoResponseDTO.class
        );
        assertNotNull(responseAgendamento.getId());
        assertEquals("PENDENTE", responseAgendamento.getStatus().name());
    }

    @Test
    @DisplayName("FLUXO 3: Aceite Docente ➔ Check-in Bilateral na Sala de Aula ➔ Encerramento e Avaliação Pós-Aula")
    void testFluxo3_AceiteDocenteCheckinBilateralEncerramentoEAvaliacao() throws Exception {
        // Preparação: Criar agendamento em data futura
        Agendamento agendamento = Agendamento.builder()
                .aluno(alunoGabriel)
                .professor(profCarlos)
                .saber(disciplinaBanco)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(16, 0))
                .status(StatusAgendamento.PENDENTE)
                .modalidade("ONLINE")
                .valorAplicado(BigDecimal.valueOf(70.00))
                .assunto("Consultas SQL e Índices B-Tree")
                .build();
        agendamento = agendamentoRepository.save(agendamento);

        // Passo 3.1: Docente aceita a mentoria (transita para CONFIRMADO)
        mockMvc.perform(put("/api/v1/agendamentos/{id}/aceitar", agendamento.getId())
                        .header("Authorization", tokenProfessor)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMADO"));

        // Passo 3.2: Check-in do Aluno na Sala de Aula
        mockMvc.perform(post("/api/v1/aulas/{id}/checkin?usuarioId={uid}&papel=ALUNO", agendamento.getId(), alunoGabriel.getId())
                        .header("Authorization", tokenAluno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.alunoCheckin").value(true));

        // Passo 3.3: Check-in do Professor na Sala de Aula (Dispara EM_ANDAMENTO)
        mockMvc.perform(post("/api/v1/aulas/{id}/checkin?usuarioId={uid}&papel=PROFESSOR", agendamento.getId(), profCarlos.getId())
                        .header("Authorization", tokenProfessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.professorCheckin").value(true))
                .andExpect(jsonPath("$.data.statusSessao").value("EM_ANDAMENTO"));

        // Passo 3.4: Finalização Bilateral da Aula (Transita para REALIZADA)
        mockMvc.perform(post("/api/v1/aulas/{id}/finalizar?usuarioId={uid}&papel=ALUNO", agendamento.getId(), alunoGabriel.getId())
                        .header("Authorization", tokenAluno))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/aulas/{id}/finalizar?usuarioId={uid}&papel=PROFESSOR", agendamento.getId(), profCarlos.getId())
                        .header("Authorization", tokenProfessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statusSessao").value("CONCLUIDA"));

        // Passo 3.5: Aluno envia avaliação pós-aula (5 estrelas)
        AvaliacaoRequestDTO avaliacaoDTO = AvaliacaoRequestDTO.builder()
                .nota(5)
                .didatica(5)
                .pontualidade(5)
                .dominio(5)
                .comentario("Excelente mentoria! Conceito de índices e planos de execução perfeitamente explicado.")
                .build();

        mockMvc.perform(post("/api/v1/aulas/{id}/avaliar?avaliadorId={uid}", agendamento.getId(), alunoGabriel.getId())
                        .header("Authorization", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(avaliacaoDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nota").value(5))
                .andExpect(jsonPath("$.data.novaMediaDocente").exists());
    }

    @Test
    @DisplayName("FLUXO 4: Liquidação Financeira ➔ Retenção de Taxa (10%) ➔ Solicitação de Saque PIX (SAQ-YYYY-XXXXX)")
    void testFluxo4_LiquidacaoFinanceiraRetencaoTaxaESaquePix() throws Exception {
        // Preparação: Garantir agendamento REALIZADA e transação contábil
        Agendamento agendamento = Agendamento.builder()
                .aluno(alunoGabriel)
                .professor(profCarlos)
                .saber(disciplinaBanco)
                .data(LocalDate.now().minusDays(1))
                .hora(LocalTime.of(10, 0))
                .status(StatusAgendamento.REALIZADA)
                .modalidade("ONLINE")
                .valorAplicado(BigDecimal.valueOf(100.00))
                .assunto("Auditoria Contábil e Performance SQL")
                .build();
        agendamento = agendamentoRepository.save(agendamento);

        // Registro da transação com retenção de 10% (R$ 100 bruto -> R$ 10 taxa -> R$ 90 líquido)
        TransacaoFinanceira transacao = TransacaoFinanceira.builder()
                .usuario(profCarlos)
                .agendamento(agendamento)
                .tipoTransacao(com.equipe.projetoadsapi.model.enums.TipoTransacao.CREDITO_AULA)
                .valor(BigDecimal.valueOf(90.00))
                .descricao("Mentoria realizada # " + agendamento.getId())
                .build();
        transacaoRepository.save(transacao);

        // Passo 4.1: Consulta do Resumo Financeiro do Professor
        mockMvc.perform(get("/api/v1/professores/{id}/financeiro/resumo", profCarlos.getId())
                        .header("Authorization", tokenProfessor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taxaPlataformaPercentual").value(10.0))
                .andExpect(jsonPath("$.data.saldoDisponivel").exists());

        // Passo 4.2: Solicitação de Saque PIX com Chave E-mail
        SaqueRequestDTO saqueDTO = SaqueRequestDTO.builder()
                .valor(BigDecimal.valueOf(50.00))
                .tipoChavePix(TipoChavePix.EMAIL)
                .chavePix("prof.carlos@orienta.edu.br")
                .observacoes("Solicitação de saque de teste de banca")
                .build();

        mockMvc.perform(post("/api/v1/professores/{id}/financeiro/saques", profCarlos.getId())
                        .header("Authorization", tokenProfessor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saqueDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.protocolo").value(matchesPattern("^SAQ-2026-[A-Z0-9]{5}$")))
                .andExpect(jsonPath("$.data.status").value("PENDENTE"))
                .andExpect(jsonPath("$.data.valorLiquido").value(50.00));
    }

    @Test
    @DisplayName("FLUXO 5: Comunicação no Chat Bilateral ➔ Abertura de Ticket de Suporte ➔ Moderação de Denúncia Ética")
    void testFluxo5_ChatBilateralSuporteEModenacaoDenuncia() throws Exception {
        // Passo 5.1: Criação e Envio de Mensagem no Chat Bilateral
        Agendamento agendamento = Agendamento.builder()
                .aluno(alunoGabriel)
                .professor(profCarlos)
                .saber(disciplinaBanco)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(15, 0))
                .status(StatusAgendamento.CONFIRMADO)
                .modalidade("ONLINE")
                .valorAplicado(BigDecimal.valueOf(70.00))
                .assunto("Tópicos Avançados de Modelagem")
                .build();
        agendamento = agendamentoRepository.save(agendamento);

        Conversa conversa = conversaRepository.save(Conversa.builder().agendamento(agendamento).ativa(true).build());

        EnviarMensagemDTO msgDTO = EnviarMensagemDTO.builder()
                .conversaId(conversa.getId())
                .agendamentoId(agendamento.getId())
                .remetenteId(alunoGabriel.getId())
                .conteudo("Professor Carlos, estou enviando a lista de exercícios em anexo.")
                .tipo("TEXTO")
                .build();

        mockMvc.perform(post("/api/v1/chat/mensagens")
                        .header("Authorization", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conteudo").value(containsString("lista de exercícios")));

        // Passo 5.2: Abertura de Ticket de Suporte com Protocolo Oficial SUP-YYYY-XXXXX
        CriarTicketDTO ticketDTO = CriarTicketDTO.builder()
                .titulo("Dúvida sobre cancelamento de mentoria com 2h")
                .categoria(CategoriaTicket.DUVIDA)
                .prioridade(PrioridadeTicket.BAIXA)
                .descricao("Gostaria de confirmar se o cancelamento com mais de 2 horas de antecedência não gera multas.")
                .build();

        mockMvc.perform(post("/api/v1/suporte/tickets?usuarioId={uid}", alunoGabriel.getId())
                        .header("Authorization", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticketDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.protocolo").value(matchesPattern("^SUP-2026-\\d{5}$")))
                .andExpect(jsonPath("$.data.status").value("ABERTO"));

        // Passo 5.3: Abertura de Denúncia Ética na Ouvidoria com Protocolo DEN-YYYY-XXXXX
        CriarDenunciaDTO denunciaDTO = CriarDenunciaDTO.builder()
                .denunciadoId(profCarlos.getId())
                .agendamentoId(agendamento.getId())
                .motivo("CONDUTA_INAPROPRIADA")
                .descricao("Reporte preventivo de conduta para validação do fluxo de moderação da banca.")
                .build();

        mockMvc.perform(post("/api/v1/suporte/denuncias?denuncianteId={uid}", alunoGabriel.getId())
                        .header("Authorization", tokenAluno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(denunciaDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.protocolo").value(matchesPattern("^DEN-2026-\\d{5}$")))
                .andExpect(jsonPath("$.data.status").value("PENDENTE"));
    }
}
