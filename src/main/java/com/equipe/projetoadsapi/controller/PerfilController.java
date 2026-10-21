package com.equipe.projetoadsapi.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;

/**
 * CONTROLLER DE GERENCIAMENTO DE PERFIL (ALUNO / PROFESSOR)
 * * Defesa para a Banca: "Este é o cérebro da integração do perfil. Ele expõe uma API RESTful. 
 * Utilizamos a anotação @CrossOrigin para derrubar a barreira do CORS, permitindo que o aplicativo 
 * React Native faça requisições com segurança. A anotação @Transactional garante a atomicidade das 
 * operações: se houver alguma falha ao salvar uma matéria, o banco sofre um rollback automático, 
 * impedindo dados corrompidos."
 */
@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "*") // 🟢 Liberação universal do CORS para o Expo/React Native conseguir conectar
public class PerfilController {

    // Injeção de Dependências automática do Spring (Padrão Inversion of Control)
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private InteresseRepository interesseRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    /**
     * SALVAR OU ATUALIZAR PERFIL DO ALUNO
     * Endpoint: PUT http://SEU_IP:8080/api/perfil/aluno/{id}
     */
    @PutMapping("/aluno/{id}")
    @Transactional // Garante que a deleção antiga e a inserção nova aconteçam como uma única transação segura
    public ResponseEntity<?> atualizarAluno(@PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        // 1. Cláusula de Guarda: Verifica se o usuário existe no banco de dados
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Atualiza os dados cadastrais básicos que o Aluno alterou na tela
        usuario.setNome(dto.getNome());
        // Caso possua os campos bio e nascimento na sua classe Usuario, descomente as linhas abaixo:
        // usuario.setBio(dto.getBio());
        // usuario.setNascimento(dto.getNascimento());
        usuarioRepository.save(usuario);

        // 3. Limpa os interesses antigos para evitar duplicidade antes de inserir os novos
        interesseRepository.deleteByUsuarioId(id);

        // 4. Processa e vincula a nova lista de interesses selecionada no aplicativo
        if (dto.getInteressesNomes() != null) {
            for (String nomeSaber : dto.getInteressesNomes()) {
                // Busca o saber no "cardápio" do banco. Se não existir, cria um novo dinamicamente.
                Saber saber = saberRepository.findByNome(nomeSaber)
                        .orElseGet(() -> saberRepository.save(new Saber(null, nomeSaber, "Geral")));

                // Instancia a tabela associativa ligando o Aluno ao Saber
                Interesse novoInteresse = new Interesse(null, usuario, saber);
                interesseRepository.save(novoInteresse);
            }
        }

        return ResponseEntity.ok().body("Perfil de Aluno atualizado com sucesso!");
    }

    /**
     * SALVAR OU ATUALIZAR PERFIL DO PROFESSOR
     * Endpoint: PUT http://SEU_IP:8080/api/perfil/professor/{id}
     */
    @PutMapping("/professor/{id}")
    @Transactional
    public ResponseEntity<?> atualizarProfessor(@PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Atualiza dados básicos e a Bio do professor
        usuario.setNome(dto.getNome());
        // usuario.setBio(dto.getBio());
        usuarioRepository.save(usuario);

        // Limpa as aptidões antigas antes de salvar a nova grade de aulas do professor
        aptidaoRepository.deleteByUsuarioId(id);

        // Processa as novas aptidões, incluindo o nível de domínio e o preço da hora digitados no Front-end
        if (dto.getAptidoes() != null) {
            for (AptidaoRequestDTO aptoDTO : dto.getAptidoes()) {
                
                Saber saber = saberRepository.findByNome(aptoDTO.getNomeSaber())
                        .orElseGet(() -> saberRepository.save(new Saber(null, aptoDTO.getNomeSaber(), "Geral")));

                // Monta a entidade Aptidao com os atributos exclusivos do professor (Preço e Experiência)
                Aptidao novaAptidao = new Aptidao(
                        null, 
                        usuario, 
                        saber, 
                        aptoDTO.getNivelDominio(), 
                        aptoDTO.getPrecoHora()
                );
                aptidaoRepository.save(novaAptidao);
            }
        }

        return ResponseEntity.ok().body("Perfil de Professor atualizado com sucesso!");
    }
}