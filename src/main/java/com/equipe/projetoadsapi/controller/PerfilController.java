package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.dto.PerfilAtualizacaoDTO;
import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.Interesse;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AptidaoRepository;
import com.equipe.projetoadsapi.repository.InteresseRepository;
import com.equipe.projetoadsapi.repository.SaberRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CONTROLLER DE PERFIL LEGADO (Compatibilidade Retroativa).
 * Rastreabilidade: Fase 4 - f4-t1, f4-t3
 * @deprecated Prefira utilizar /api/v1/usuarios/{id}/perfil para leitura e atualização consolidada.
 */
@Deprecated(since = "v1")
@RestController
@RequestMapping({"/api/v1/perfil", "/api/perfil"})
@Tag(name = "Perfil Legado (Compatibilidade)", description = "Endpoints de compatibilidade com versões antigas do mobile. Recomenda-se migrar para /api/v1/usuarios/{id}/perfil.")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private InteresseRepository interesseRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    @Operation(summary = "Atualizar perfil aluno (legado)", deprecated = true)
    @PutMapping("/aluno/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> atualizarAluno(
            @PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));

        usuario.setNome(dto.getNome());
        usuarioRepository.save(usuario);

        interesseRepository.deleteByUsuarioId(id);

        if (dto.getInteresses() != null) {
            for (String nomeMateria : dto.getInteresses()) {
                Saber saber = saberRepository.findByNome(nomeMateria)
                        .orElseGet(() -> {
                            Saber novo = new Saber();
                            novo.setNome(nomeMateria);
                            novo.setCategoria("Geral");
                            return saberRepository.save(novo);
                        });

                Interesse interesse = new Interesse();
                interesse.setUsuario(usuario);
                interesse.setSaber(saber);
                interesseRepository.save(interesse);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensagem", "Perfil do aluno atualizado com sucesso!");
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @Operation(summary = "Atualizar perfil professor (legado)", deprecated = true)
    @PutMapping("/professor/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> atualizarProfessor(
            @PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));

        usuario.setNome(dto.getNome());
        usuarioRepository.save(usuario);

        aptidaoRepository.deleteByUsuarioId(id);

        if (dto.getAptidoes() != null) {
            for (var item : dto.getAptidoes()) {
                Saber saber = saberRepository.findByNome(item.getMateria())
                        .orElseGet(() -> {
                            Saber novo = new Saber();
                            novo.setNome(item.getMateria());
                            novo.setCategoria("Geral");
                            return saberRepository.save(novo);
                        });

                Aptidao aptidao = new Aptidao();
                aptidao.setUsuario(usuario);
                aptidao.setSaber(saber);
                aptidao.setPreco(item.getPreco());
                aptidaoRepository.save(aptidao);
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("mensagem", "Perfil do professor atualizado com sucesso!");
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @Operation(summary = "Obter dados do perfil aluno (legado)", deprecated = true)
    @GetMapping("/aluno/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterAluno(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<String> nomesInteresses = interesses.stream()
                .map(i -> i.getSaber().getNome())
                .toList();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", usuario.getNome());
        dados.put("email", usuario.getEmail());
        dados.put("interesses", nomesInteresses);

        return ResponseEntity.ok(ApiResponse.ok(dados));
    }

    @Operation(summary = "Obter dados do perfil professor (legado)", deprecated = true)
    @GetMapping("/professor/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterProfessor(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));

        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Map<String, Object>> listaAptidoes = aptidoes.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("materia", a.getSaber().getNome());
            map.put("preco", a.getPreco());
            return map;
        }).toList();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", usuario.getNome());
        dados.put("email", usuario.getEmail());
        dados.put("aptidoes", listaAptidoes);

        return ResponseEntity.ok(ApiResponse.ok(dados));
    }
}