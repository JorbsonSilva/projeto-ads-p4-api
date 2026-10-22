package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * SERVIÇO DE EXECUÇÃO DE AULA AO VIVO, CRONÔMETRO BILATERAL E AVALIAÇÕES.
 * Rastreabilidade: Fase 6 - f6-t1, f6-t2, f6-t3, f6-t4, f6-t6, f6-t8
 */
@Service
public class AulaService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private SessaoAulaRepository sessaoRepository;

    @Autowired
    private AulaEventoRepository eventoRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public SessaoAulaDTO obterOuCriarSessao(Long agendamentoId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + agendamentoId));

        // Regra de Negócio: Apenas aulas CONFIRMADAS, EM_ANDAMENTO ou AGUARDANDO_CONFIRMACAO podem ter sessão
        if (agendamento.getStatus() != StatusAgendamento.CONFIRMADO && 
            agendamento.getStatus() != StatusAgendamento.EM_ANDAMENTO &&
            agendamento.getStatus() != StatusAgendamento.AGUARDANDO_CONFIRMACAO &&
            agendamento.getStatus() != StatusAgendamento.REALIZADA) {
            throw new RegraNegocioException("Apenas aulas com status CONFIRMADO podem ter sessão iniciada. Status atual: " + agendamento.getStatus());
        }

        SessaoAula sessao = sessaoRepository.findByAgendamentoId(agendamentoId)
                .orElseGet(() -> {
                    SessaoAula nova = SessaoAula.builder()
                            .agendamento(agendamento)
                            .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                            .alunoCheckin(false)
                            .professorCheckin(false)
                            .build();
                    return sessaoRepository.save(nova);
                });

        return SessaoAulaDTO.fromEntity(sessao);
    }

    /**
     * Check-in bilateral (aluno e professor dão check-in para disparar cronômetro oficial)
     */
    @Transactional
    public SessaoAulaDTO realizarCheckin(Long agendamentoId, Long usuarioId) {
        SessaoAulaDTO dto = obterOuCriarSessao(agendamentoId);
        SessaoAula sessao = sessaoRepository.findById(dto.getId()).orElseThrow();
        Agendamento agendamento = sessao.getAgendamento();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId));

        boolean isAluno = agendamento.getAluno().getId().equals(usuarioId);
        boolean isProfessor = agendamento.getProfessor().getId().equals(usuarioId);

        if (!isAluno && !isProfessor) {
            throw new RegraNegocioException("Apenas o aluno ou o professor vinculados à aula podem realizar check-in.");
        }

        Instant agora = Instant.now();

        if (isAluno && Boolean.FALSE.equals(sessao.getAlunoCheckin())) {
            sessao.setAlunoCheckin(true);
            sessao.setAlunoCheckinEm(agora);
            registrarEvento(sessao, TipoEventoAula.CHECKIN_ALUNO, usuario, "Check-in do aluno confirmado", 0);
        }

        if (isProfessor && Boolean.FALSE.equals(sessao.getProfessorCheckin())) {
            sessao.setProfessorCheckin(true);
            sessao.setProfessorCheckinEm(agora);
            registrarEvento(sessao, TipoEventoAula.CHECKIN_PROFESSOR, usuario, "Check-in do professor confirmado", 0);
        }

        // Se ambos deram check-in, o cronômetro do servidor é disparado (início bilateral)
        if (Boolean.TRUE.equals(sessao.getAlunoCheckin()) && Boolean.TRUE.equals(sessao.getProfessorCheckin()) 
                && sessao.getStatusSessao() == StatusSessao.AGUARDANDO_CHECKIN) {
            sessao.setStatusSessao(StatusSessao.EM_ANDAMENTO);
            sessao.setInicioEfetivo(agora);
            agendamento.setStatus(StatusAgendamento.EM_ANDAMENTO);
            agendamentoRepository.save(agendamento);

            registrarEvento(sessao, TipoEventoAula.INICIO_AULA, null, "Início bilateral confirmado: cronômetro oficial ativo", 0);
        }

        SessaoAula salva = sessaoRepository.save(sessao);
        return SessaoAulaDTO.fromEntity(salva);
    }

    /**
     * Pausa de aula por consentimento mútuo
     */
    @Transactional
    public SessaoAulaDTO pausarAula(Long agendamentoId, Long usuarioId, String motivo) {
        SessaoAula sessao = sessaoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada para o agendamento: " + agendamentoId));

        if (sessao.getStatusSessao() != StatusSessao.EM_ANDAMENTO) {
            throw new RegraNegocioException("Apenas aulas em andamento podem ser pausadas.");
        }

        Usuario autor = usuarioRepository.findById(usuarioId).orElse(null);
        sessao.setStatusSessao(StatusSessao.PAUSADA);
        sessao.setUltimaPausaEm(Instant.now());

        int decorrido = calcularSegundosDecorridos(sessao);
        registrarEvento(sessao, TipoEventoAula.PAUSA, autor, motivo != null ? motivo : "Pausa solicitada", decorrido);

        SessaoAula salva = sessaoRepository.save(sessao);
        return SessaoAulaDTO.fromEntity(salva);
    }

    /**
     * Retomada de aula pausada
     */
    @Transactional
    public SessaoAulaDTO retomarAula(Long agendamentoId, Long usuarioId) {
        SessaoAula sessao = sessaoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada: " + agendamentoId));

        if (sessao.getStatusSessao() != StatusSessao.PAUSADA) {
            throw new RegraNegocioException("Apenas aulas com status PAUSADA podem ser retomadas.");
        }

        Usuario autor = usuarioRepository.findById(usuarioId).orElse(null);
        if (sessao.getUltimaPausaEm() != null) {
            long deltaPausa = Duration.between(sessao.getUltimaPausaEm(), Instant.now()).getSeconds();
            sessao.setSegundosPausados((sessao.getSegundosPausados() != null ? sessao.getSegundosPausados() : 0) + (int) deltaPausa);
            sessao.setUltimaPausaEm(null);
        }
        sessao.setStatusSessao(StatusSessao.EM_ANDAMENTO);

        int decorrido = calcularSegundosDecorridos(sessao);
        registrarEvento(sessao, TipoEventoAula.RETOMADA, autor, "Aula retomada", decorrido);

        SessaoAula salva = sessaoRepository.save(sessao);
        return SessaoAulaDTO.fromEntity(salva);
    }

    /**
     * Finalização bilateral de aula (mútua concordância)
     */
    @Transactional
    public SessaoAulaDTO finalizarAula(Long agendamentoId, Long usuarioId) {
        SessaoAula sessao = sessaoRepository.findByAgendamentoId(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão não encontrada: " + agendamentoId));

        Agendamento agendamento = sessao.getAgendamento();
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId));

        boolean isAluno = agendamento.getAluno().getId().equals(usuarioId);
        boolean isProfessor = agendamento.getProfessor().getId().equals(usuarioId);

        if (!isAluno && !isProfessor) {
            throw new RegraNegocioException("Apenas os participantes da aula podem finalizar a sessão.");
        }

        if (isAluno) sessao.setAlunoConfirmouFim(true);
        if (isProfessor) sessao.setProfessorConfirmouFim(true);

        int decorrido = calcularSegundosDecorridos(sessao);

        // Se apenas um confirmou, vai para AGUARDANDO_CONFIRMACAO
        if (Boolean.TRUE.equals(sessao.getAlunoConfirmouFim()) ^ Boolean.TRUE.equals(sessao.getProfessorConfirmouFim())) {
            sessao.setStatusSessao(StatusSessao.AGUARDANDO_CONFIRMACAO);
            agendamento.setStatus(StatusAgendamento.AGUARDANDO_CONFIRMACAO);
            agendamentoRepository.save(agendamento);

            registrarEvento(sessao, TipoEventoAula.SOLICITACAO_FIM, usuario, "Encerramento solicitado por " + usuario.getNome(), decorrido);
        }

        // Se ambos confirmaram, conclusão definitiva
        if (Boolean.TRUE.equals(sessao.getAlunoConfirmouFim()) && Boolean.TRUE.equals(sessao.getProfessorConfirmouFim())) {
            sessao.setStatusSessao(StatusSessao.CONCLUIDA);
            sessao.setFimEfetivo(Instant.now());
            sessao.setDuracaoTotalSegundos(decorrido);

            agendamento.setStatus(StatusAgendamento.REALIZADA);
            agendamentoRepository.save(agendamento);

            registrarEvento(sessao, TipoEventoAula.CONFIRMACAO_FIM, usuario, "Aula concluída com consentimento mútuo!", decorrido);
        }

        SessaoAula salva = sessaoRepository.save(sessao);
        return SessaoAulaDTO.fromEntity(salva);
    }

    /**
     * Avaliação pós-aula (liberada exclusivamente após atingir REALIZADA)
     */
    @Transactional
    public AvaliacaoResponseDTO avaliarAula(Long agendamentoId, Long avaliadorId, AvaliacaoRequestDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));

        // Regra de Negócio: Avaliação só é liberada se status for REALIZADA
        if (agendamento.getStatus() != StatusAgendamento.REALIZADA) {
            throw new RegraNegocioException("A avaliação de aula só é liberada após a conclusão mútua (status REALIZADA). Status atual: " + agendamento.getStatus());
        }

        Usuario avaliador = usuarioRepository.findById(avaliadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário avaliador não encontrado: " + avaliadorId));

        // Impede avaliação duplicada pelo mesmo usuário na mesma aula
        if (avaliacaoRepository.existsByAgendamentoIdAndAvaliadorId(agendamentoId, avaliadorId)) {
            throw new RegraNegocioException("Você já enviou sua avaliação para esta aula.");
        }

        Usuario avaliado;
        if (agendamento.getAluno().getId().equals(avaliadorId)) {
            avaliado = agendamento.getProfessor();
        } else if (agendamento.getProfessor().getId().equals(avaliadorId)) {
            avaliado = agendamento.getAluno();
        } else {
            throw new RegraNegocioException("Apenas o aluno ou o professor desta aula podem enviar avaliações.");
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .agendamento(agendamento)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(dto.getNota())
                .pontualidade(dto.getPontualidade() != null ? dto.getPontualidade() : dto.getNota())
                .didatica(dto.getDidatica() != null ? dto.getDidatica() : dto.getNota())
                .comentario(dto.getComentario())
                .build();

        Avaliacao salva = avaliacaoRepository.save(avaliacao);

        // Atualiza a média de notas e total de avaliações do avaliado
        atualizarMediaUsuario(avaliado);

        return AvaliacaoResponseDTO.fromEntity(salva);
    }

    private void atualizarMediaUsuario(Usuario usuario) {
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByAvaliadoId(usuario.getId());
        if (!avaliacoes.isEmpty()) {
            double soma = avaliacoes.stream().mapToInt(Avaliacao::getNota).sum();
            double media = soma / avaliacoes.size();
            usuario.setNotaMedia(Math.round(media * 10.0) / 10.0);
            usuario.setTotalAvaliacoes(avaliacoes.size());
            usuarioRepository.save(usuario);
        }
    }

    private int calcularSegundosDecorridos(SessaoAula sessao) {
        if (sessao.getInicioEfetivo() == null) return 0;
        if (sessao.getStatusSessao() == StatusSessao.CONCLUIDA && sessao.getDuracaoTotalSegundos() != null && sessao.getDuracaoTotalSegundos() > 0) {
            return sessao.getDuracaoTotalSegundos();
        }
        Instant refTempo = Instant.now();
        if (sessao.getStatusSessao() == StatusSessao.PAUSADA && sessao.getUltimaPausaEm() != null) {
            refTempo = sessao.getUltimaPausaEm();
        } else if (sessao.getFimEfetivo() != null) {
            refTempo = sessao.getFimEfetivo();
        }
        long segundos = Duration.between(sessao.getInicioEfetivo(), refTempo).getSeconds();
        int pausado = sessao.getSegundosPausados() != null ? sessao.getSegundosPausados() : 0;
        return (int) Math.max(0, segundos - pausado);
    }

    private void registrarEvento(SessaoAula sessao, TipoEventoAula tipo, Usuario autor, String motivo, int decorrido) {
        AulaEvento evento = AulaEvento.builder()
                .sessao(sessao)
                .tipoEvento(tipo)
                .autor(autor)
                .motivo(motivo)
                .segundosDecorridos(decorrido)
                .build();
        eventoRepository.save(evento);
    }
}
