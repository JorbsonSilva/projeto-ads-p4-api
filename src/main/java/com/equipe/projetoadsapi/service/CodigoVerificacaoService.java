package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.CodigoVerificacao;
import com.equipe.projetoadsapi.repository.CodigoVerificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Serviço de emissão e conferência de códigos com prazo de expiração (15 minutos).
 * Rastreabilidade: Fase 3 (f3-t9)
 */
@Service
public class CodigoVerificacaoService {

    private static final int EXPIRACAO_MINUTOS = 15;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private CodigoVerificacaoRepository repository;

    @Transactional
    public String gerarECadastrarCodigo(String email, String tipo) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacao registro = CodigoVerificacao.builder()
                .email(email)
                .codigo(codigo)
                .tipo(tipo)
                .expiraEm(LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS))
                .utilizado(false)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(registro);
        return codigo;
    }

    @Transactional
    public void validarEConsumirCodigo(String email, String tipo, String codigoInformado) {
        CodigoVerificacao registro = repository
                .findTopByEmailAndTipoAndUtilizadoFalseOrderByCreatedAtDesc(email, tipo)
                .orElseThrow(() -> new RegraNegocioException("Nenhum código ativo encontrado para o e-mail informado."));

        if (registro.isExpirado()) {
            throw new RegraNegocioException("O código de verificação expirou. Solicite um novo código.");
        }

        if (!registro.getCodigo().trim().equals(codigoInformado.trim())) {
            throw new RegraNegocioException("Código de verificação incorreto!");
        }

        registro.setUtilizado(true);
        repository.save(registro);
    }
}
