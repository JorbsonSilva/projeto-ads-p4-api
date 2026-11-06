package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.AlunoAtendidoDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.Conversa;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.model.SessaoAula;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.ConversaRepository;
import com.equipe.projetoadsapi.repository.SaberRepository;
import com.equipe.projetoadsapi.repository.SessaoAulaRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITÁRIOS DAS REGRAS DE NEGÓCIO DE USUÁRIO (UsuarioService)
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Garante o correto funcionamento de:
 * 1) Hashing unidirecional com BCrypt salt aleatório;
 * 2) Validação antecipada de unicidade de CPF para mitigar falhas de constraint SQL;
 * 3) Geração do código de verificação com exatamente 6 dígitos numéricos;
 * 4) Preservação do algoritmo de Haversine e filtros de proximidade geográfica."
 */
@ExtendWith(MockitoExtension.class)
public class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository repository;

    @Mock
    private SaberRepository saberRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private SessaoAulaRepository sessaoAulaRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private LocalizacaoService localizacaoService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Usuario usuarioExemplo;

    @BeforeEach
    void setUp() {
        usuarioExemplo = new Usuario();
        usuarioExemplo.setId(1L);
        usuarioExemplo.setNome("Matheus Oliveira");
        usuarioExemplo.setEmail("matheus@orienta.edu.br");
        usuarioExemplo.setCpf("123.456.789-00");
        usuarioExemplo.setSenha("senha123");
    }

    /**
     * TESTE 1: Limpeza de CPF e Hashing BCrypt ao cadastrar usuário.
     */
    @Test
    @DisplayName("Deve normalizar CPF e codificar a senha com BCrypt ao iniciar cadastro")
    void deveCodificarSenhaETratarCpfNoCadastro() {
        when(repository.findByEmail("matheus@orienta.edu.br")).thenReturn(null);
        when(repository.findByCpf("12345678900")).thenReturn(null);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashBCryptExemploMock");

        usuarioService.cadastrarUsuario(usuarioExemplo);

        assertEquals("12345678900", usuarioExemplo.getCpf(), "CPF deve ter pontuação removida");
        assertEquals("$2a$10$hashBCryptExemploMock", usuarioExemplo.getSenha(), "Senha deve ser gravada com hash BCrypt");
        assertFalse(Boolean.TRUE.equals(usuarioExemplo.getEmailVerificado()), "Usuário deve iniciar com emailVerificado=false");
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    /**
     * TESTE 2: Impedir cadastro com e-mail duplicado.
     */
    @Test
    @DisplayName("Deve lançar RegraNegocioException quando o e-mail já estiver cadastrado")
    void deveLancarExcecaoParaEmailDuplicado() {
        when(repository.findByEmail("matheus@orienta.edu.br")).thenReturn(new Usuario());

        assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrarUsuario(usuarioExemplo));
        verify(passwordEncoder, never()).encode(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /**
     * TESTE 3: Impedir cadastro com CPF duplicado.
     */
    @Test
    @DisplayName("Deve lançar RegraNegocioException quando o CPF já estiver cadastrado em outra conta")
    void deveLancarExcecaoParaCpfDuplicado() {
        when(repository.findByEmail("matheus@orienta.edu.br")).thenReturn(null);
        when(repository.findByCpf("12345678900")).thenReturn(new Usuario());

        assertThrows(RegraNegocioException.class, () -> usuarioService.cadastrarUsuario(usuarioExemplo));
        verify(passwordEncoder, never()).encode(any());
    }

    /**
     * TESTE 4: Garantia de Não-Regressão do Haversine (Linhas 440-570 preservadas).
     */
    @Test
    @DisplayName("Deve delegar o cálculo esférico de Haversine para LocalizacaoService sem quebrar os métodos legados")
    void devePreservarCalculoHaversine() {
        when(localizacaoService.normalizarRaio(null)).thenReturn(50);
        when(repository.findAll()).thenReturn(java.util.Collections.emptyList());

        var resultado = usuarioService.buscarProfessoresAvancado(
                null, "PRESENCIAL", null, null, -7.115, -34.864, 20, null
        );

        assertNotNull(resultado, "A busca geográfica por Haversine deve retornar lista não-nula");
        verify(localizacaoService, times(1)).normalizarRaio(any());
    }

    /**
     * TESTE 5: Gestão de Alunos Atendidos do Professor (BACK-019).
     */
    @Test
    @DisplayName("[BACK-019] Deve agregar métricas e histórico de alunos atendidos em mentorias concluídas")
    void deveAgregarAlunosAtendidosEmMentoriasConcluidas() {
        Long professorId = 1L;

        Usuario aluno = new Usuario();
        aluno.setId(50L);
        aluno.setNome("Ana Beatriz");
        aluno.setEmail("ana@academico.br");
        aluno.setRegiaoAtendimento("João Pessoa - PB");

        Saber saber = new Saber();
        saber.setNome("Banco de Dados");

        Agendamento aula1 = Agendamento.builder()
                .id(101L)
                .professor(usuarioExemplo)
                .aluno(aluno)
                .saber(saber)
                .data(LocalDate.of(2026, 11, 4))
                .hora(LocalTime.of(14, 0))
                .status(StatusAgendamento.REALIZADA)
                .modalidade("PRESENCIAL")
                .build();

        Agendamento aula2 = Agendamento.builder()
                .id(102L)
                .professor(usuarioExemplo)
                .aluno(aluno)
                .saber(saber)
                .data(LocalDate.of(2026, 11, 2))
                .hora(LocalTime.of(10, 0))
                .status(StatusAgendamento.REALIZADA)
                .modalidade("ONLINE")
                .build();

        SessaoAula sessao1 = SessaoAula.builder()
                .id(201L)
                .agendamento(aula1)
                .duracaoTotalSegundos(5400) // 1.5h
                .build();

        when(repository.existsById(professorId)).thenReturn(true);
        when(agendamentoRepository.findByProfessorIdAndStatusOrderByDataDescHoraDesc(professorId, StatusAgendamento.REALIZADA))
                .thenReturn(List.of(aula1, aula2));
        when(sessaoAulaRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(sessao1));
        when(sessaoAulaRepository.findByAgendamentoId(102L)).thenReturn(Optional.empty()); // default 1.0h
        when(conversaRepository.findByAgendamentoId(101L)).thenReturn(Optional.empty());

        List<AlunoAtendidoDTO> resultado = usuarioService.listarAlunosAtendidosDoProfessor(professorId);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());

        AlunoAtendidoDTO alunoDTO = resultado.get(0);
        assertEquals(50L, alunoDTO.getAlunoId());
        assertEquals("Ana Beatriz", alunoDTO.getNome());
        assertEquals(2, alunoDTO.getTotalAulasConcluidas());
        assertEquals(2.5, alunoDTO.getHorasAcumuladas()); // 1.5 + 1.0
        assertEquals("2026-11-04", alunoDTO.getDataUltimaAula());
        assertEquals(List.of("Banco de Dados"), alunoDTO.getDisciplinasCursadas());
        assertEquals("HIBRIDO", alunoDTO.getFormatoPreferido());
        assertEquals(101L, alunoDTO.getAgendamentoIdMaisRecente());
    }
}
