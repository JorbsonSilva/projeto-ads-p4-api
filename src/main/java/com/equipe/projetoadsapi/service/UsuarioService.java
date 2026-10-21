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
        mailSender.send(msg);
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