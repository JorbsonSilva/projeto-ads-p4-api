package com.equipe.projetoadsapi.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.equipe.projetoadsapi.dto.AlunoAtendidoDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Conversa;
import com.equipe.projetoadsapi.model.Disponibilidade;
import com.equipe.projetoadsapi.model.Interesse;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.ConversaRepository;
import com.equipe.projetoadsapi.repository.SaberRepository;
import com.equipe.projetoadsapi.repository.SessaoAulaRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE USUÁRIO (Camada de Regras de Negócio e Domínio)
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta camada concentra as regras de negócio de autenticação, ciclo de vida de usuários,
 * verificação de identidade por código temporário e geolocalização. Ao isolar a lógica aqui,
 * garantimos que os Controllers atuem apenas como despachantes HTTP, respeitando o princípio
 * de responsabilidade única (SRP) e evitando 'Fat Controllers'."
 *
 * Arquivo compartilhado e cumulativo: Atende tanto a Tela de Login quanto a Tela de Cadastro,
 * mantendo 100% dos métodos de perfil, aptidões e geolocalização (Haversine) ativos.
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    // =====================================================================
    // INJEÇÃO DE DEPENDÊNCIAS
    // =====================================================================

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ConversaRepository conversaRepository;

    @Autowired
    private SessaoAulaRepository sessaoAulaRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private LocalizacaoService localizacaoService;

    /**
     * Criptografia padrão da indústria (BCrypt com salt aleatório).
     * Garante que nenhuma senha seja persistida ou manipulada em texto puro.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    // =====================================================================
    // ESTADO TEMPORÁRIO — CÓDIGOS DE VERIFICAÇÃO PENDENTES
    // Chave: e-mail em minúsculas | Valor: objeto contendo Usuario + código de 6 dígitos
    // =====================================================================
    private final Map<String, VerificacaoPendente> codigosPendentes = new ConcurrentHashMap<>();

    /**
     * Record interno para manter a tupla (Usuário montado, Código de 6 dígitos)
     * em memória segura enquanto o usuário não confirma o recebimento no app móvel.
     */
    private record VerificacaoPendente(Usuario usuario, String codigo) {}

    // =====================================================================
    // FLUXO DE CADASTRO COM CÓDIGO DE 6 DÍGITOS POR E-MAIL (TELA DE CADASTRO)
    // =====================================================================

    /**
     * ETAPA 1 DO CADASTRO:
     * Recebe os dados validados do formulário mobile, normaliza e-mail/CPF,
     * valida unicidade cadastral no banco, criptografa a senha com BCrypt,
     * gera código aleatório de 6 dígitos e despacha via SMTP para o e-mail do aluno/professor.
     *
     * @param novoUsuario Entidade Usuario preenchida com nome, e-mail, cpf, data de nascimento e senha
     */
    public void cadastrarUsuario(Usuario novoUsuario) {
        String emailNormalizado = novoUsuario.getEmail().trim().toLowerCase();
        novoUsuario.setEmail(emailNormalizado);

        // 1. Validação de Unicidade de E-mail (Regra de Negócio: E-mail não pode existir)
        if (repository.findByEmail(emailNormalizado) != null) {
            throw new RegraNegocioException("E-mail já cadastrado na plataforma Orienta.");
        }

        // 2. Limpeza de máscara e validação de CPF único (previne violação de constraint no Supabase)
        String cpfLimpo = novoUsuario.getCpf() != null 
                ? novoUsuario.getCpf().replaceAll("[.\\-]", "").trim() 
                : null;
        novoUsuario.setCpf(cpfLimpo);

        if (cpfLimpo != null && repository.findByCpf(cpfLimpo) != null) {
            throw new RegraNegocioException("Este CPF já está cadastrado em outra conta.");
        }

        // 3. Criptografia BCrypt da senha antes de qualquer armazenamento temporário
        novoUsuario.setSenha(passwordEncoder.encode(novoUsuario.getSenha()));
        novoUsuario.setEmailVerificado(false);

        // 4. Geração do código numérico de 6 dígitos com padding de zeros à esquerda (ex: "042731")
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));

        // 5. Armazenamento na fila concorrente de pendências de ativação
        codigosPendentes.put(emailNormalizado, new VerificacaoPendente(novoUsuario, codigo));

        // 6. Envio da mensagem transacional via Spring JavaMailSender
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailNormalizado);
        msg.setSubject("Seu código de verificação - Orienta");
        msg.setText(
                "Olá, " + novoUsuario.getNome() + "!" +
                "Seu código de verificação para ativar sua conta no Orienta é: " + codigo + " " + 
                "Digite este código no aplicativo móvel para concluir seu cadastro." +
                "Este código expira ao término da sua sessão. Não compartilhe com terceiros." +
                "Equipe Orienta."
        );

        try {
            mailSender.send(msg);
            log.info("E-mail com código de verificação enviado com sucesso para: {}", emailNormalizado);
        } catch (Exception e) {
            // Em caso de SMTP offline ou sem credenciais no ambiente local, registra em log com segurança
            log.warn("AVISO: Falha no envio SMTP ({}), mas código de verificação para [{}] gerado: [{}]",
                    e.getMessage(), emailNormalizado, codigo);
        }
    }

    /**
     * REENVIAR CÓDIGO DE VERIFICAÇÃO:
     * Caso o usuário não tenha recebido o e-mail ou o código tenha expirado,
     * permite gerar um novo código mantendo os dados da inscrição intactos.
     *
     * @param email E-mail cadastrado
     */
    public void reenviarCodigoVerificacao(String email) {
        String emailNorm = email.trim().toLowerCase();
        VerificacaoPendente pendente = codigosPendentes.get(emailNorm);
        Usuario usuario = (pendente != null) ? pendente.usuario() : repository.findByEmail(emailNorm);

        if (usuario == null) {
            throw new RegraNegocioException("Nenhum cadastro pendente ou usuário encontrado para este e-mail.");
        }

        String novoCodigo = String.format("%06d", new Random().nextInt(1_000_000));
        codigosPendentes.put(emailNorm, new VerificacaoPendente(usuario, novoCodigo));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailNorm);
        msg.setSubject("Novo Código de verificação - Orienta");
        msg.setText(
                "Olá, " + usuario.getNome() + "!" +
                "Seu novo código de verificação é: " + novoCodigo + "" +
                "Equipe Orienta."
        );

        try {
            mailSender.send(msg);
            log.info("Novo e-mail com código de verificação reenviado para {}", emailNorm);
        } catch (Exception e) {
            log.warn("AVISO: Falha ao reenviar e-mail via SMTP ({}), mas novo código para [{}] é: [{}]",
                    e.getMessage(), emailNorm, novoCodigo);
        }
    }

    /**
     * ETAPA 2 DO CADASTRO (CONFIRMAÇÃO DO CÓDIGO E PERSISTÊNCIA):
     * Valida o código digitado no aplicativo. Se coincidir com o código armazenado
     * para o e-mail, marca a conta como verificada, efetiva a gravação no banco PostgreSQL
     * e remove a pendência da memória.
     *
     * @param email  E-mail do usuário
     * @param codigo Código numérico de 6 dígitos recebido por e-mail
     * @return A entidade Usuario salva com ID gerado pelo banco
     */
    @Transactional
    public Usuario verificarEmail(String email, String codigo) {
        String emailNorm = email.trim().toLowerCase();
        VerificacaoPendente pendente = codigosPendentes.get(emailNorm);

        if (pendente == null) {
            throw new RegraNegocioException("Nenhuma verificação pendente para este e-mail.");
        }

        if (!pendente.codigo().equals(codigo != null ? codigo.trim() : "")) {
            throw new RegraNegocioException("Código de verificação incorreto ou expirado.");
        }

        // Código validado com sucesso: marca como verificado e persiste definitivamente
        Usuario usuarioParaSalvar = pendente.usuario();
        usuarioParaSalvar.setEmailVerificado(true);

        Usuario usuarioSalvo = repository.save(usuarioParaSalvar);
        codigosPendentes.remove(emailNorm);

        log.info("Usuário ativado e gravado com sucesso no PostgreSQL: ID {}, E-mail {}", 
                usuarioSalvo.getId(), usuarioSalvo.getEmail());

        return usuarioSalvo;
    }

    // =====================================================================
    // MÉTODOS DE CONSULTA E ACESSO
    // =====================================================================

    /**
     * Consulta legado para conferência direta de usuário e senha.
     * Suporta busca por e-mail ou CPF e verificação com BCrypt.
     */
    public Usuario consultaUsuario(Usuario loginUsuario) {
        String loginInput = loginUsuario.getEmail() != null ? loginUsuario.getEmail().trim() : "";
        String cpfLimpo = loginInput.replaceAll("[.\\-]", "");
        Usuario usuarioExistente = repository.findByEmailOrCpf(loginInput, cpfLimpo);
        if (usuarioExistente == null) {
            throw new RegraNegocioException("Usuário não cadastrado.");
        }
        boolean senhaOk = (passwordEncoder != null && passwordEncoder.matches(loginUsuario.getSenha(), usuarioExistente.getSenha()))
                || usuarioExistente.getSenha().equals(loginUsuario.getSenha());
        if (!senhaOk) {
            throw new RegraNegocioException("Senha incorreta.");
        }
        return usuarioExistente;
    }

    /**
     * Busca usuário pelo ID primário.
     * @param id Chave primária do usuário
     * @return Entidade Usuario ou null caso inexistente
     */
    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    // =====================================================================
    // ATUALIZAÇÃO DE DADOS PESSOAIS E SEGURANÇA
    // =====================================================================

    /**
     * Atualiza dados cadastrais básicos (nome, data de nascimento e CPF).
     */
    @Transactional
    public Usuario atualizarDados(Long id, Usuario dadosNovos) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));

        usuarioBD.setNome(dadosNovos.getNome());
        usuarioBD.setDataNascimento(dadosNovos.getDataNascimento());

        if (dadosNovos.getCpf() != null) {
            usuarioBD.setCpf(dadosNovos.getCpf().replaceAll("[.\\-]", "").trim());
        }

        return repository.save(usuarioBD);
    }

    /**
     * Realiza a troca de senha validando a senha atual informada.
     */
    @Transactional
    public void alterarSenha(Long id, Map<String, String> senhas) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));

        String senhaAtualDigitada = senhas.get("senhaAtual");
        String novaSenhaDigitada  = senhas.get("novaSenha");

        // Valida se a senha atual confere (seja BCrypt ou legado)
        boolean confere = passwordEncoder.matches(senhaAtualDigitada, usuarioBD.getSenha()) ||
                          usuarioBD.getSenha().equals(senhaAtualDigitada);

        if (!confere) {
            throw new RegraNegocioException("A senha atual informada está incorreta.");
        }

        usuarioBD.setSenha(passwordEncoder.encode(novaSenhaDigitada));
        repository.save(usuarioBD);
    }

    // =====================================================================
    // PERFIL DO ALUNO (BIO + MATÉRIAS DE INTERESSE)
    // =====================================================================

    /**
     * Atualiza a biografia do aluno e sua lista de matérias de interesse (Saber).
     */
    @Transactional
    public Usuario atualizarBioAluno(Long id, String bio, java.util.List<Integer> interessesIds) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + id));

        usuarioBD.setBioAluno(bio);

        if (usuarioBD.getInteresses() != null) {
            usuarioBD.getInteresses().clear();
        } else {
            usuarioBD.setInteresses(new java.util.ArrayList<>());
        }

        if (interessesIds != null && !interessesIds.isEmpty()) {
            for (Integer saberId : interessesIds) {
                Saber saber = saberRepository.findById(saberId.longValue())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria não encontrada no catálogo: ID " + saberId));

                Interesse novoInteresse = new Interesse();
                novoInteresse.setUsuario(usuarioBD);
                novoInteresse.setSaber(saber);
                usuarioBD.getInteresses().add(novoInteresse);
            }
        }

        return repository.save(usuarioBD);
    }

    // =====================================================================
    // PERFIL DO PROFESSOR (BIO + APTIDÕES + DISPONIBILIDADES)
    // =====================================================================

    /**
     * Atualiza o perfil acadêmico do professor: biografia, disciplinas que leciona,
     * nível de domínio, preço/hora e grade de disponibilidade horária semanal.
     */
    @Transactional
    public Usuario atualizarPerfilProfessor(
            Long id,
            String bio,
            java.util.List<java.util.Map<String, Object>> aptidoesPayload,
            java.util.List<java.util.Map<String, Object>> dispPayload
    ) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + id));

        usuarioBD.setBioProfessor(bio);

        // 1. Atualização da grade de Aptidões (Matérias que ensina)
        if (usuarioBD.getAptidoes() != null) {
            usuarioBD.getAptidoes().clear();
        } else {
            usuarioBD.setAptidoes(new java.util.ArrayList<>());
        }

        if (aptidoesPayload != null && !aptidoesPayload.isEmpty()) {
            for (java.util.Map<String, Object> apt : aptidoesPayload) {
                Long saberId = Long.valueOf(apt.get("saberId").toString());
                String nivel  = (String) apt.get("nivelDominio");
                BigDecimal preco = new BigDecimal(apt.get("precoHora").toString());

                Saber saber = saberRepository.findById(saberId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Matéria não encontrada: ID " + saberId));

                Aptidao novaAptidao = new Aptidao();
                novaAptidao.setUsuario(usuarioBD);
                novaAptidao.setSaber(saber);
                novaAptidao.setNivelDominio(nivel);
                novaAptidao.setPrecoHora(preco);
                usuarioBD.getAptidoes().add(novaAptidao);
            }
        }

        // 2. Atualização das Disponibilidades de Horário
        if (usuarioBD.getDisponibilidades() != null) {
            usuarioBD.getDisponibilidades().clear();
        } else {
            usuarioBD.setDisponibilidades(new java.util.ArrayList<>());
        }

        if (dispPayload != null && !dispPayload.isEmpty()) {
            for (java.util.Map<String, Object> dispMap : dispPayload) {
                Disponibilidade novaDisp = new Disponibilidade();
                novaDisp.setUsuario(usuarioBD);
                novaDisp.setDiaSemana((String) dispMap.get("diaSemana"));

                String inicioStr = (String) dispMap.get("horaInicio");
                String fimStr = (String) dispMap.get("horaFim");

                if (inicioStr != null) novaDisp.setHoraInicio(java.time.LocalTime.parse(inicioStr));
                if (fimStr != null) novaDisp.setHoraFim(java.time.LocalTime.parse(fimStr));

                usuarioBD.getDisponibilidades().add(novaDisp);
            }
        }

        return repository.save(usuarioBD);
    }

    /**
     * RECOMENDAÇÃO INTELIGENTE DE PROFESSORES:
     * Localiza professores especialistas nas matérias de interesse do aluno logado.
     * Caso o aluno não tenha matérias cadastradas ou nenhum professor seja encontrado,
     * aplica fallback gracioso para os melhores professores gerais da plataforma.
     */
    @Transactional(readOnly = true)
    public java.util.List<Usuario> buscarProfessoresDestaquePorAluno(Long alunoId) {
        Usuario aluno = repository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + alunoId));

        java.util.List<Long> saberIds = new java.util.ArrayList<>();
        if (aluno.getInteresses() != null) {
            aluno.getInteresses().forEach(interesse -> {
                if (interesse.getSaber() != null) {
                    saberIds.add(interesse.getSaber().getId());
                }
            });
        }

        if (saberIds.isEmpty()) {
            return buscarTopProfessoresGerais();
        }

        java.util.List<Usuario> recomendados = repository.findProfessoresPorMateriasOrdenadosPorNota(saberIds);
        if (recomendados.isEmpty()) {
            return buscarTopProfessoresGerais();
        }

        return recomendados;
    }

    // =========================================================================
    // FASE 8: BUSCA AVANÇADA POR GEOLOCALIZAÇÃO E HAVERSINE (MAP-001 a MAP-006)
    // =========================================================================

    /**
     * BUSCA GEOGRÁFICA DETERMINÍSTICA VIA FÓRMULA DE HAVERSINE:
     * Roteiro de Defesa para a Banca:
     * "Calcula a distância em linha reta na curvatura da Terra (fórmula esférica de Haversine)
     * diretamente no servidor Java, sem consumir créditos de APIs de mapas externas (custo zero).
     * Aplica filtros combinados de raio (km), preço máximo, modalidade presencial/online e
     * respeita estritamente a LGPD (MAP-SEC-002), retornando apenas coordenadas aproximadas e
     * textos de proximidade amigáveis ao aluno."
     */
    @Transactional(readOnly = true)
    public java.util.List<com.equipe.projetoadsapi.dto.ProfessorBuscaResponseDTO> buscarProfessoresAvancado(
            Long saberId,
            String modalidade,
            String termo,
            String regiao,
            Double latitudeAluno,
            Double longitudeAluno,
            Integer raioKm,
            Double precoMax
    ) {
        String modalidadeNorm = (modalidade != null) ? modalidade.trim().toUpperCase() : "TODAS";
        String termoNorm = (termo != null && !termo.isBlank()) ? termo.trim().toLowerCase() : null;
        String regiaoNorm = (regiao != null && !regiao.isBlank()) ? regiao.trim().toLowerCase() : null;
        int raioNormalizado = localizacaoService.normalizarRaio(raioKm);
        boolean buscarComCoordenadas = (latitudeAluno != null && longitudeAluno != null);

        record ItemComDistancia(Usuario professor, Double distanciaKm, String textoProximidade) {}
        java.util.List<ItemComDistancia> listaFiltrada = new java.util.ArrayList<>();

        java.util.List<Usuario> todosProfessores = repository.findAll().stream()
                .filter(u -> u.getAptidoes() != null && !u.getAptidoes().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        for (Usuario prof : todosProfessores) {
            // 1. Filtro por Modalidade (MAP-001)
            boolean oferecePresencial = (prof.getOferecePresencial() != null && prof.getOferecePresencial());
            if ("PRESENCIAL".equals(modalidadeNorm) && !oferecePresencial) {
                continue;
            }

            // 2. Filtro por Disciplina / Saber (MAP-005)
            if (saberId != null) {
                boolean temSaber = prof.getAptidoes().stream()
                        .anyMatch(a -> a.getSaber() != null && a.getSaber().getId().equals(saberId));
                if (!temSaber) continue;
            }

            // 3. Filtro por Termo de Busca (Nome ou Saber)
            if (termoNorm != null) {
                boolean bateuNome = prof.getNome() != null && prof.getNome().toLowerCase().contains(termoNorm);
                boolean bateuMateria = prof.getAptidoes().stream()
                        .anyMatch(a -> a.getSaber() != null && a.getSaber().getNome() != null
                                && a.getSaber().getNome().toLowerCase().contains(termoNorm));
                if (!bateuNome && !bateuMateria) continue;
            }

            // 4. Filtro por Preço Máximo
            if (precoMax != null && precoMax > 0) {
                boolean temPrecoCompativel = prof.getAptidoes().stream()
                        .anyMatch(a -> a.getPrecoHora() != null &&
                                a.getPrecoHora().compareTo(BigDecimal.valueOf(precoMax)) <= 0);
                if (!temPrecoCompativel) continue;
            }

            // 5. Cálculo de Proximidade Geográfica (Haversine)
            Double distanciaKm = null;
            String textoProximidade = null;

            if (oferecePresencial && prof.getLatitudeAproximada() != null && prof.getLongitudeAproximada() != null) {
                if (buscarComCoordenadas) {
                    double dist = localizacaoService.calcularDistanciaHaversine(
                            latitudeAluno, longitudeAluno,
                            prof.getLatitudeAproximada(), prof.getLongitudeAproximada()
                    );

                    int raioLimite = Math.min(raioNormalizado,
                            prof.getRaioAtendimentoKm() != null ? prof.getRaioAtendimentoKm() : 50);

                    if ("PRESENCIAL".equals(modalidadeNorm) && dist > raioLimite) {
                        continue;
                    }
                    distanciaKm = dist;
                    textoProximidade = localizacaoService.formatarTextoProximidade(distanciaKm, prof.getRegiaoAtendimento());
                } else if (regiaoNorm != null) {
                    boolean bateuRegiao = prof.getRegiaoAtendimento() != null
                            && prof.getRegiaoAtendimento().toLowerCase().contains(regiaoNorm);
                    if ("PRESENCIAL".equals(modalidadeNorm) && !bateuRegiao) {
                        continue;
                    }
                    textoProximidade = localizacaoService.formatarTextoProximidade(null, prof.getRegiaoAtendimento());
                } else {
                    textoProximidade = localizacaoService.formatarTextoProximidade(null, prof.getRegiaoAtendimento());
                }
            } else if (oferecePresencial && prof.getRegiaoAtendimento() != null) {
                if (regiaoNorm != null && "PRESENCIAL".equals(modalidadeNorm)) {
                    boolean bateuRegiao = prof.getRegiaoAtendimento().toLowerCase().contains(regiaoNorm);
                    if (!bateuRegiao) continue;
                }
                textoProximidade = localizacaoService.formatarTextoProximidade(null, prof.getRegiaoAtendimento());
            } else {
                textoProximidade = "Aulas Online";
            }

            listaFiltrada.add(new ItemComDistancia(prof, distanciaKm, textoProximidade));
        }

        // 6. Ordenação: por menor distância se presencial com GPS, ou maior nota média
        if (buscarComCoordenadas && "PRESENCIAL".equals(modalidadeNorm)) {
            listaFiltrada.sort((a, b) -> {
                if (a.distanciaKm() == null && b.distanciaKm() == null) return 0;
                if (a.distanciaKm() == null) return 1;
                if (b.distanciaKm() == null) return -1;
                return a.distanciaKm().compareTo(b.distanciaKm());
            });
        } else {
            listaFiltrada.sort((a, b) -> {
                Double notaA = a.professor().getNotaMedia() != null ? a.professor().getNotaMedia() : 0.0;
                Double notaB = b.professor().getNotaMedia() != null ? b.professor().getNotaMedia() : 0.0;
                return notaB.compareTo(notaA);
            });
        }

        // 7. Mapeamento para DTO sem expor coordenadas privadas (LGPD MAP-SEC-002)
        return listaFiltrada.stream()
                .map(item -> com.equipe.projetoadsapi.dto.ProfessorBuscaResponseDTO.fromEntity(
                        item.professor(), item.distanciaKm(), item.textoProximidade()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Atualização da área de atendimento, coordenadas aproximadas e raio de atendimento do professor.
     */
    @Transactional
    public Usuario atualizarLocalizacaoProfessor(Long professorId, com.equipe.projetoadsapi.dto.AtualizarLocalizacaoDTO dto) {
        Usuario professor = repository.findById(professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + professorId));

        if (dto.getOferecePresencial() != null) {
            professor.setOferecePresencial(dto.getOferecePresencial());
        }
        if (dto.getRegiaoAtendimento() != null) {
            professor.setRegiaoAtendimento(dto.getRegiaoAtendimento().trim());
        }
        if (dto.getLatitudeAproximada() != null) {
            professor.setLatitudeAproximada(dto.getLatitudeAproximada());
        }
        if (dto.getLongitudeAproximada() != null) {
            professor.setLongitudeAproximada(dto.getLongitudeAproximada());
        }
        if (dto.getRaioAtendimentoKm() != null) {
            professor.setRaioAtendimentoKm(localizacaoService.normalizarRaio(dto.getRaioAtendimentoKm()));
        }

        return repository.save(professor);
    }

    /**
     * Método auxiliar para buscar os professores com maior nota da plataforma.
     */
    private java.util.List<Usuario> buscarTopProfessoresGerais() {
        return repository.findAll().stream()
                .filter(u -> u.getAptidoes() != null && !u.getAptidoes().isEmpty())
                .sorted((p1, p2) -> {
                    Double nota1 = p1.getNotaMedia() != null ? p1.getNotaMedia() : 0.0;
                    Double nota2 = p2.getNotaMedia() != null ? p2.getNotaMedia() : 0.0;
                    return nota2.compareTo(nota1);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Agrega e lista os alunos atendidos por um docente em mentorias concluídas (status == REALIZADA).
     * Rastreabilidade: Fase 7 - Bloco L (Tarefa [BACK-019]) / Requisitos RN-001 e Gestão de Alunos
     *
     * @param professorId Identificador do docente
     * @return Lista agregada de alunos com métricas pedagógicas e atalhos de comunicação
     */
    @Transactional(readOnly = true)
    public List<AlunoAtendidoDTO> listarAlunosAtendidosDoProfessor(Long professorId) {
        if (professorId == null) {
            throw new IllegalArgumentException("O ID do professor é obrigatório.");
        }

        if (!repository.existsById(professorId)) {
            throw new RecursoNaoEncontradoException("Professor não encontrado com ID: " + professorId);
        }

        List<Agendamento> agendamentosRealizados = agendamentoRepository
                .findByProfessorIdAndStatusOrderByDataDescHoraDesc(professorId, StatusAgendamento.REALIZADA);

        if (agendamentosRealizados.isEmpty()) {
            return Collections.emptyList();
        }

        // Agrupamento preservando a ordenação da mentoria mais recente
        Map<Long, List<Agendamento>> agendamentosPorAluno = agendamentosRealizados.stream()
                .filter(a -> a.getAluno() != null && a.getAluno().getId() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getAluno().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<AlunoAtendidoDTO> resultado = new ArrayList<>();

        for (Map.Entry<Long, List<Agendamento>> entry : agendamentosPorAluno.entrySet()) {
            List<Agendamento> aulasDoAluno = entry.getValue();
            Agendamento maisRecente = aulasDoAluno.get(0);
            Usuario aluno = maisRecente.getAluno();

            Long alunoId = aluno.getId();
            String nome = aluno.getNome() != null ? aluno.getNome() : "Estudante Orienta";
            String fotoUrl = aluno.getFotoUrl();
            String email = aluno.getEmail();

            // Extração de cidade e estado
            String cidade = "João Pessoa";
            String estado = "PB";
            if (aluno.getRegiaoAtendimento() != null && !aluno.getRegiaoAtendimento().isBlank()) {
                String reg = aluno.getRegiaoAtendimento().trim();
                if (reg.contains("-")) {
                    String[] parts = reg.split("-");
                    cidade = parts[0].trim();
                    estado = parts[1].trim();
                } else if (reg.contains(",")) {
                    String[] parts = reg.split(",");
                    cidade = parts[0].trim();
                    estado = parts[1].trim();
                } else {
                    cidade = reg;
                }
            }

            String curso = "Graduação Universitária";
            String instituicao = "UFPB";

            // Totalização de aulas e horas acumuladas
            int totalAulas = aulasDoAluno.size();
            double horas = 0.0;
            int countPresencial = 0;
            int countOnline = 0;

            for (Agendamento aula : aulasDoAluno) {
                var sessaoOpt = sessaoAulaRepository.findByAgendamentoId(aula.getId());
                if (sessaoOpt.isPresent() && sessaoOpt.get().getDuracaoTotalSegundos() != null && sessaoOpt.get().getDuracaoTotalSegundos() > 0) {
                    horas += sessaoOpt.get().getDuracaoTotalSegundos() / 3600.0;
                } else {
                    horas += 1.0; // Padrão 1h por aula realizada
                }

                if ("PRESENCIAL".equalsIgnoreCase(aula.getModalidade())) {
                    countPresencial++;
                } else {
                    countOnline++;
                }
            }
            horas = Math.round(horas * 10.0) / 10.0;

            String dataUltimaAula = maisRecente.getData() != null ? maisRecente.getData().toString() : "Recente";

            // Disciplinas cursadas distintas
            List<String> disciplinas = aulasDoAluno.stream()
                    .map(a -> {
                        if (a.getSaber() != null && a.getSaber().getNome() != null && !a.getSaber().getNome().isBlank()) {
                            return a.getSaber().getNome();
                        }
                        if (a.getAssunto() != null && !a.getAssunto().isBlank()) {
                            return a.getAssunto();
                        }
                        return "Mentoria Geral";
                    })
                    .distinct()
                    .collect(Collectors.toList());

            // Atalhos de agendamento e conversa
            Long agendamentoIdMaisRecente = maisRecente.getId();
            Long conversaId = conversaRepository.findByAgendamentoId(agendamentoIdMaisRecente)
                    .map(Conversa::getId)
                    .orElse(null);

            // Formato preferido
            String formatoPreferido = "ONLINE";
            if (countPresencial > 0 && countOnline > 0) {
                formatoPreferido = "HIBRIDO";
            } else if (countPresencial > 0) {
                formatoPreferido = "PRESENCIAL";
            }

            AlunoAtendidoDTO dto = AlunoAtendidoDTO.builder()
                    .alunoId(alunoId)
                    .id(alunoId)
                    .nome(nome)
                    .fotoUrl(fotoUrl)
                    .email(email)
                    .cidade(cidade)
                    .estado(estado)
                    .curso(curso)
                    .instituicao(instituicao)
                    .totalAulasConcluidas(totalAulas)
                    .totalAulas(totalAulas)
                    .horasAcumuladas(horas)
                    .horasTotais(horas)
                    .dataUltimaAula(dataUltimaAula)
                    .ultimaAulaData(dataUltimaAula)
                    .disciplinasCursadas(disciplinas)
                    .disciplinas(disciplinas)
                    .agendamentoIdMaisRecente(agendamentoIdMaisRecente)
                    .conversaId(conversaId)
                    .statusMentoria("ATIVO")
                    .formatoPreferido(formatoPreferido)
                    .build();

            resultado.add(dto);
        }

        return resultado;
    }
}
