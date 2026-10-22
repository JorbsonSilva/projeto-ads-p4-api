package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.NotificacaoDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.Notificacao;
import com.equipe.projetoadsapi.model.TipoNotificacao;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.NotificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE NOTIFICAÇÕES INTERNAS (Custo Zero, Polling-friendly).
 * Rastreabilidade: Fase 7 - f7-t5
 */
@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Transactional
    public Notificacao criarNotificacao(Usuario usuario, String titulo, String mensagem, TipoNotificacao tipo, Long referenciaId) {
        if (usuario == null) return null;
        Notificacao n = Notificacao.builder()
                .usuario(usuario)
                .titulo(titulo)
                .mensagem(mensagem)
                .tipo(tipo)
                .referenciaId(referenciaId)
                .lida(false)
                .dataHora(Instant.now())
                .build();
        return notificacaoRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDTO> listarNotificacoes(Long usuarioId) {
        return notificacaoRepository.findTop20ByUsuarioIdOrderByDataHoraDesc(usuarioId)
                .stream()
                .map(NotificacaoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(Long usuarioId) {
        return notificacaoRepository.countByUsuarioIdAndLidaFalse(usuarioId);
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao n = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada: " + notificacaoId));
        if (n.getUsuario().getId().equals(usuarioId)) {
            n.setLida(true);
            notificacaoRepository.save(n);
        }
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        notificacaoRepository.marcarTodasComoLidas(usuarioId);
    }
}
