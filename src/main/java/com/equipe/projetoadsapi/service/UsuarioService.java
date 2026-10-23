package com.equipe.projetoadsapi.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.UsuarioRepository;


/**
 * SERVIÇO DE USUÁRIO (Camada de Regras de Negócio)
 * * Roteiro de Defesa para a Banca: "Esta camada concentra toda a inteligência da aplicação.
 * Ao isolar as regras de negócio aqui, evitamos o anti-pattern 'Fat Controller'.
 * O fluxo de cadastro usa verificação por e-mail em duas etapas: o usuário recebe um
 * código de 6 dígitos e somente após confirmá-lo é que o registro é persistido no banco."
 */
@Service
public class UsuarioService {

    // =====================================================================
    // DEPENDÊNCIAS
    // =====================================================================

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private com.equipe.projetoadsapi.repository.SaberRepository saberRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private LocalizacaoService localizacaoService;

    // =====================================================================
    // ESTADO INTERNO — Códigos de verificação pendentes
    // Chave: e-mail do usuário | Valor: objeto com os dados + código gerado
    // =====================================================================

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UsuarioService.class);

    // =====================================================================
    // ESTADO INTERNO — Códigos de verificação pendentes
    // Chave: e-mail do usuário | Valor: objeto com os dados + código gerado
    // =====================================================================

    private final Map<String, VerificacaoPendente> codigosPendentes = new ConcurrentHashMap<>();

    /** Estrutura interna para guardar o usuário completo enquanto aguarda verificação. */
    private record VerificacaoPendente(Usuario usuario, String codigo) {}

    // =====================================================================
    // FLUXO DE CADASTRO COM VERIFICAÇÃO DE E-MAIL
    // =====================================================================

    /**
     * ETAPA 1: Recebe os dados do novo usuário, gera um código de 6 dígitos,
     * armazena em memória e envia o código por e-mail.
     * O registro NÃO é salvo no banco até a confirmação do código.
     */
    public void cadastrarUsuario(Usuario novoUsuario) {
        if (repository.findByEmail(novoUsuario.getEmail()) != null) {
            throw new RuntimeException("Email já cadastrado!!!");
        }

        // Gera um código numérico de 6 dígitos com zero-padding (ex: "042731")
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));

        // Guarda o objeto Usuario completo + código na memória temporária
        codigosPendentes.put(novoUsuario.getEmail(), new VerificacaoPendente(novoUsuario, codigo));

        // Envia o e-mail de verificação
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(novoUsuario.getEmail());
        msg.setSubject("Código de verificação - Orienta");
        msg.setText(
                "Olá, " + novoUsuario.getNome() + "!\n\n" +
                        "Seu código de verificação é: " + codigo + "\n\n" +
                        "Este código é válido durante a sessão atual. Não compartilhe com ninguém."
        );
        try {
            mailSender.send(msg);
            log.info("E-mail com código de verificação enviado para {}", novoUsuario.getEmail());
        } catch (Exception e) {
            log.warn("AVISO: Falha ao despachar e-mail via SMTP ({}), mas código de verificação para [{}] gerado com sucesso: [{}]",
                    e.getMessage(), novoUsuario.getEmail(), codigo);
        }
    }

    /**
     * REENVIAR CÓDIGO: Gera um novo código para uma verificação em andamento.
     */
    public void reenviarCodigoVerificacao(String email) {
        VerificacaoPendente pendente = codigosPendentes.get(email);
        Usuario usuario = (pendente != null) ? pendente.usuario() : repository.findByEmail(email);

        if (usuario == null) {
            throw new RuntimeException("Nenhum cadastro pendente ou usuário encontrado para este e-mail.");
        }

        String novoCodigo = String.format("%06d", new Random().nextInt(1_000_000));
        codigosPendentes.put(email, new VerificacaoPendente(usuario, novoCodigo));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Novo Código de verificação - Orienta");
        msg.setText(
                "Olá, " + usuario.getNome() + "!\n\n" +
                        "Seu novo código de verificação é: " + novoCodigo + "\n\n" +
                        "Este código é válido durante a sessão atual. Não compartilhe com ninguém."
        );
        try {
            mailSender.send(msg);
            log.info("Novo e-mail com código de verificação reenviado para {}", email);
        } catch (Exception e) {
            log.warn("AVISO: Falha ao reenviar e-mail via SMTP ({}), mas novo código para [{}] é: [{}]",
                    e.getMessage(), email, novoCodigo);
        }
    }

    /**
     * ETAPA 2: Valida o código digitado pelo usuário.
     * Se correto, persiste o registro no banco e limpa o código pendente.
     *
     * @param email  E-mail informado no cadastro
     * @param codigo Código de 6 dígitos recebido por e-mail
     * @return O usuário recém-criado (com ID gerado pelo banco)
     */
    @Transactional
    public Usuario verificarEmail(String email, String codigo) {
        VerificacaoPendente pendente = codigosPendentes.get(email);

        if (pendente == null) {
            throw new RuntimeException("Nenhuma verificação pendente para este e-mail.");
        }
        if (!pendente.codigo().equals(codigo)) {
            throw new RuntimeException("Código inválido ou expirado.");
        }

        // Código correto: salva o usuário completo e remove da fila
        Usuario usuarioSalvo = repository.save(pendente.usuario());
        codigosPendentes.remove(email);
        return usuarioSalvo;
    }

    // =====================================================================
    // AUTENTICAÇÃO
    // =====================================================================

    public Usuario consultaUsuario(Usuario loginUsuario) {
        Usuario usuarioExistente = repository.findByEmail(loginUsuario.getEmail());
        if (usuarioExistente == null) {
            throw new RuntimeException("Usuário não cadastrado!!!");
        } else if (!usuarioExistente.getSenha().equals(loginUsuario.getSenha())) {
            throw new RuntimeException("Senha incorreta!!!");
        }
        return usuarioExistente;
    }

    // =====================================================================
    // BUSCA
    // =====================================================================

    public Usuario buscarPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    // =====================================================================
    // ATUALIZAÇÃO DE DADOS PESSOAIS
    // =====================================================================

    @Transactional
    public Usuario atualizarDados(Long id, Usuario dadosNovos) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        usuarioBD.setNome(dadosNovos.getNome());
        usuarioBD.setDataNascimento(dadosNovos.getDataNascimento());
        usuarioBD.setCpf(dadosNovos.getCpf());

        return repository.save(usuarioBD);
    }

    // =====================================================================
    // TROCA DE SENHA
    // =====================================================================

    @Transactional
    public void alterarSenha(Long id, Map<String, String> senhas) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        String senhaAtualDigitada = senhas.get("senhaAtual");
        String novaSenhaDigitada  = senhas.get("novaSenha");

        if (!usuarioBD.getSenha().equals(senhaAtualDigitada)) {
            throw new RuntimeException("A senha atual informada está incorreta!");
        }

        usuarioBD.setSenha(novaSenhaDigitada);
        repository.save(usuarioBD);
    }

    // =====================================================================
    // PERFIL DO ALUNO (Bio + Interesses)
    // =====================================================================

    @Transactional
    public Usuario atualizarBioAluno(Long id, String bio, java.util.List<Integer> interessesIds) {
        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        usuarioBD.setBioAluno(bio);

        if (usuarioBD.getInteresses() != null) {
            usuarioBD.getInteresses().clear();
        } else {
            usuarioBD.setInteresses(new java.util.ArrayList<>());
        }

        if (interessesIds != null && !interessesIds.isEmpty()) {
            for (Integer saberId : interessesIds) {
                com.equipe.projetoadsapi.model.Saber saber = saberRepository.findById(saberId.longValue())
                        .orElseThrow(() -> new RuntimeException("Matéria não encontrada no catálogo."));

                com.equipe.projetoadsapi.model.Interesse novoInteresse = new com.equipe.projetoadsapi.model.Interesse();
                novoInteresse.setUsuario(usuarioBD);
                novoInteresse.setSaber(saber);

                usuarioBD.getInteresses().add(novoInteresse);
            }
        }

        return repository.save(usuarioBD);
    }

// =====================================================================
    // PERFIL DO PROFESSOR (Bio + Aptidões + Disponibilidades)
    // =====================================================================

    @Transactional
    public Usuario atualizarPerfilProfessor(Long id, String bio,
                                            java.util.List<java.util.Map<String, Object>> aptidoesPayload,
                                            java.util.List<java.util.Map<String, Object>> dispPayload) { // 🟢 Adicionado dispPayload

        Usuario usuarioBD = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        usuarioBD.setBioProfessor(bio);

        // --- 1. LÓGICA DE APTIDÕES (MANTIDA IGUAL À SUA ORIGINAL) ---
        if (usuarioBD.getAptidoes() != null) {
            usuarioBD.getAptidoes().clear();
        } else {
            usuarioBD.setAptidoes(new java.util.ArrayList<>());
        }

        if (aptidoesPayload != null && !aptidoesPayload.isEmpty()) {
            for (java.util.Map<String, Object> apt : aptidoesPayload) {
                Long saberId = Long.valueOf(apt.get("saberId").toString());
                String nivel  = (String) apt.get("nivelDominio");
                java.math.BigDecimal preco = new java.math.BigDecimal(apt.get("precoHora").toString());

                com.equipe.projetoadsapi.model.Saber saber = saberRepository.findById(saberId)
                        .orElseThrow(() -> new RuntimeException("Matéria não encontrada no catálogo."));

                com.equipe.projetoadsapi.model.Aptidao novaAptidao = new com.equipe.projetoadsapi.model.Aptidao();
                novaAptidao.setUsuario(usuarioBD);
                novaAptidao.setSaber(saber);
                novaAptidao.setNivelDominio(nivel);
                novaAptidao.setPrecoHora(preco);

                usuarioBD.getAptidoes().add(novaAptidao);
            }
        }

        // --- 2. 🟢 NOVA LÓGICA DE DISPONIBILIDADES ---
        if (usuarioBD.getDisponibilidades() != null) {
            usuarioBD.getDisponibilidades().clear();
        } else {
            usuarioBD.setDisponibilidades(new java.util.ArrayList<>());
        }

        if (dispPayload != null && !dispPayload.isEmpty()) {
            for (java.util.Map<String, Object> dispMap : dispPayload) {
                com.equipe.projetoadsapi.model.Disponibilidade novaDisp = new com.equipe.projetoadsapi.model.Disponibilidade();
                novaDisp.setUsuario(usuarioBD);
                novaDisp.setDiaSemana((String) dispMap.get("diaSemana"));
                
                String inicioStr = (String) dispMap.get("horaInicio");
                String fimStr = (String) dispMap.get("horaFim");
                
                // Converte a string "08:00:00" que veio do React Native para o relógio do Java
                if (inicioStr != null) novaDisp.setHoraInicio(java.time.LocalTime.parse(inicioStr));
                if (fimStr != null) novaDisp.setHoraFim(java.time.LocalTime.parse(fimStr));
                
                usuarioBD.getDisponibilidades().add(novaDisp);
            }
        }

        return repository.save(usuarioBD);
    }
    
    // 🟢 REGRA DE NEGÓCIO: Buscar professores com base nos interesses
    @Transactional(readOnly = true)
    public java.util.List<Usuario> buscarProfessoresDestaquePorAluno(Long alunoId) {
        Usuario aluno = repository.findById(alunoId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado."));

        java.util.List<Long> saberIds = new java.util.ArrayList<>();
        
        if (aluno.getInteresses() != null) {
            aluno.getInteresses().forEach(interesse -> {
                saberIds.add(interesse.getSaber().getId()); 
            });
        }

        // Se o aluno não tem interesse nenhum cadastrado
        if (saberIds.isEmpty()) {
            return buscarTopProfessoresGerais();
        }

        // Busca no banco os professores das matérias do aluno
        java.util.List<Usuario> recomendados = repository.findProfessoresPorMateriasOrdenadosPorNota(saberIds);

        // 🟢 O SEGREDO: Se não achar nenhum professor pra matéria exata dele, traz os melhores do app
        if (recomendados.isEmpty()) {
            return buscarTopProfessoresGerais();
        }

        return recomendados;
    }

    // =========================================================================
    // FASE 8: BUSCA AVANÇADA POR LOCALIZAÇÃO, RAIO E MODALIDADE (MAP-001 a MAP-006)
    // =========================================================================

    /**
     * Busca professores aplicando critérios de modalidade, proximidade geográfica (Haversine),
     * filtro de região textual (sem GPS) e faixa de preço.
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

        // Record auxiliar para manter a tupla (Professor, Distância) durante a filtragem
        record ItemComDistancia(Usuario professor, Double distanciaKm, String textoProximidade) {}

        java.util.List<ItemComDistancia> listaFiltrada = new java.util.ArrayList<>();

        // Todos os professores cadastrados no sistema com aptidões
        java.util.List<Usuario> todosProfessores = repository.findAll().stream()
                .filter(u -> u.getAptidoes() != null && !u.getAptidoes().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        for (Usuario prof : todosProfessores) {
            // 1. Filtro por Modalidade (MAP-001)
            boolean oferecePresencial = (prof.getOferecePresencial() != null && prof.getOferecePresencial());
            if ("PRESENCIAL".equals(modalidadeNorm) && !oferecePresencial) {
                continue; // Pula professor que só dá aula online
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
                        .anyMatch(a -> a.getPrecoHora() != null && a.getPrecoHora() <= precoMax);
                if (!temPrecoCompativel) continue;
            }

            // 5. Cálculo de Distância Geográfica e Filtro de Raio (MAP-003, MAP-005, MAP-006)
            Double distanciaKm = null;
            String textoProximidade = null;

            if (oferecePresencial && prof.getLatitudeAproximada() != null && prof.getLongitudeAproximada() != null) {
                if (buscarComCoordenadas) {
                    // Cálculo no servidor via Haversine a custo zero
                    double dist = localizacaoService.calcularDistanciaHaversine(
                            latitudeAluno, longitudeAluno,
                            prof.getLatitudeAproximada(), prof.getLongitudeAproximada()
                    );

                    // Verifica se o professor está dentro do raio requisitado pelo aluno
                    // e dentro do raio máximo de deslocamento do próprio professor
                    int raioLimite = Math.min(raioNormalizado,
                            prof.getRaioAtendimentoKm() != null ? prof.getRaioAtendimentoKm() : 50);

                    if ("PRESENCIAL".equals(modalidadeNorm) && dist > raioLimite) {
                        continue; // Exclui professores fora do raio de proximidade
                    }

                    distanciaKm = dist;
                    textoProximidade = localizacaoService.formatarTextoProximidade(distanciaKm, prof.getRegiaoAtendimento());
                } else if (regiaoNorm != null) {
                    // Fallback sem GPS: busca textual por bairro ou região (MAP-008, MAP-SEC-002)
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

        // 6. Ordenação inteligente: Se busca por raio, ordena por proximidade; caso contrário, por nota média
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

        // 7. Mapeamento final para DTO sem expor coordenadas precisas (LGPD MAP-SEC-002)
        return listaFiltrada.stream()
                .map(item -> com.equipe.projetoadsapi.dto.ProfessorBuscaResponseDTO.fromEntity(
                        item.professor(), item.distanciaKm(), item.textoProximidade()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Atualização da área de atendimento e modalidade presencial do professor (MAP-002, MAP-009).
     */
    @Transactional
    public Usuario atualizarLocalizacaoProfessor(Long professorId, com.equipe.projetoadsapi.dto.AtualizarLocalizacaoDTO dto) {
        Usuario professor = repository.findById(professorId)
                .orElseThrow(() -> new com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException(
                        "Professor não encontrado com ID: " + professorId));

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
     * Atualiza dados de pagamento fictício/demonstrativo (PROP-FIN-01 / Decisão Técnica 18 da Baseline 2.0.1).
     * Armazena apenas bandeira e 4 últimos dígitos mascarados. Nunca armazena CVV.
     */
    @Transactional
    public Usuario atualizarCartaoFicticio(Long usuarioId, com.equipe.projetoadsapi.dto.CartaoFicticioDTO dto) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException("Usuário não encontrado"));

        usuario.setCartaoFicticioTitular(dto.getTitular().trim().toUpperCase());
        usuario.setCartaoFicticioBandeira(dto.resolverBandeira());
        usuario.setCartaoFicticioUltimosDigitos(dto.extrairUltimosQuatroDigitos());

        return repository.save(usuario);
    }

    // Método auxiliar para buscar os melhores gerais do app
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
}