package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.dto.PerfilAtualizacaoDTO;
import com.equipe.projetoadsapi.dto.PerfilCompletoDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/**
 * CONTROLLER DE PERFIL HARMONIZADO POR PERSONA (ALUNO E PROFESSOR).
 * Rastreabilidade: Fase 4 - f4-t1, f4-t3 & Tarefa [BACK-004] (Requisito RN-001)
 * 
 * Atende às rotas específicas por persona:
 * - GET/PUT /api/v1/perfil/aluno/{id} (preferências de estudo, biografia e dados básicos)
 * - GET/PUT /api/v1/perfil/professor/{id} (biografia, matérias lecionadas, valor/hora e disponibilidades)
 * - GET /api/v1/perfil/{id} (perfil consolidado universal)
 * 
 * Todas as respostas retornam o envelope padronizado ApiResponse<PerfilCompletoDTO>.
 */
@RestController
@RequestMapping({"/api/v1/perfil", "/api/perfil"})
@Tag(name = "Perfil por Persona (v1)", description = "Endpoints especializados para visualização e edição de perfil de Aluno e Professor")
@SecurityRequirement(name = "bearerAuth")
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private InteresseRepository interesseRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    @Autowired
    private DisponibilidadeRepository disponibilidadeRepository;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Obter perfil unificado por ID", description = "Retorna o perfil completo unificado com saberes e disponibilidades.")
    @PreAuthorize("@resourceOwnershipService.isDonoDoRecurso(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> obterPerfilPorId(@PathVariable Long id) {
        ownershipService.validarPropriedadeUsuario(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com ID: " + id));

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByUsuarioId(id);

        PerfilCompletoDTO dto = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes, disponibilidades);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Perfil consolidado recuperado com sucesso"));
    }

    @Operation(summary = "Obter dados do perfil aluno", description = "Retorna perfil com dados cadastrais e disciplinas de interesse do aluno.")
    @PreAuthorize("(hasRole('ALUNO') and @resourceOwnershipService.isDonoDoRecurso(#id)) or hasRole('ADMIN')")
    @GetMapping("/aluno/{id}")
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> obterAluno(@PathVariable Long id) {
        ownershipService.validarAcessoAluno(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + id));

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByUsuarioId(id);

        PerfilCompletoDTO dto = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes, disponibilidades);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Perfil de aluno recuperado com sucesso"));
    }

    @Operation(summary = "Obter dados do perfil professor", description = "Retorna perfil com dados pedagógicos, valores hora/aula e disponibilidade.")
    @PreAuthorize("(hasRole('PROFESSOR') and @resourceOwnershipService.isDonoDoRecurso(#id)) or hasRole('ADMIN')")
    @GetMapping("/professor/{id}")
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> obterProfessor(@PathVariable Long id) {
        ownershipService.validarAcessoDocente(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + id));

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByUsuarioId(id);

        PerfilCompletoDTO dto = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes, disponibilidades);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Perfil de professor recuperado com sucesso"));
    }

    @Operation(summary = "Atualizar perfil aluno", description = "Atualiza nome, biografia e lista de interesses do aluno logado.")
    @PreAuthorize("(hasRole('ALUNO') and @resourceOwnershipService.isDonoDoRecurso(#id)) or hasRole('ADMIN')")
    @PutMapping("/aluno/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> atualizarAluno(
            @PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        ownershipService.validarAcessoAluno(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + id));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome().trim());
        }
        if (dto.getBioEfetiva() != null) {
            usuario.setBioAluno(dto.getBioEfetiva().trim());
        }
        if (dto.getDataNascimentoEfetiva() != null && !dto.getDataNascimentoEfetiva().isBlank()) {
            usuario.setDataNascimento(dto.getDataNascimentoEfetiva().trim());
        }
        if (dto.getFotoUrl() != null && !dto.getFotoUrl().isBlank()) {
            usuario.setFotoUrl(dto.getFotoUrl().trim());
        }
        usuarioRepository.save(usuario);

        // Atualização de matérias de interesse (se enviadas)
        if (dto.getInteresses() != null) {
            interesseRepository.deleteByUsuarioId(id);
            for (String nomeMateria : dto.getInteresses()) {
                if (nomeMateria == null || nomeMateria.isBlank()) continue;
                String materiaLimpa = nomeMateria.trim();
                Saber saber = saberRepository.findByNome(materiaLimpa)
                        .orElseGet(() -> {
                            Saber novo = new Saber();
                            novo.setNome(materiaLimpa);
                            novo.setCategoria("Geral");
                            return saberRepository.save(novo);
                        });

                Interesse interesse = new Interesse();
                interesse.setUsuario(usuario);
                interesse.setSaber(saber);
                interesseRepository.save(interesse);
            }
        }

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByUsuarioId(id);

        PerfilCompletoDTO completo = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes, disponibilidades);
        return ResponseEntity.ok(ApiResponse.ok(completo, "Perfil do aluno atualizado com sucesso!"));
    }

    @Operation(summary = "Atualizar perfil professor", description = "Atualiza nome, biografia, disciplinas lecionadas, valor/hora, presencial e disponibilidade.")
    @PreAuthorize("(hasRole('PROFESSOR') and @resourceOwnershipService.isDonoDoRecurso(#id)) or hasRole('ADMIN')")
    @PutMapping("/professor/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<PerfilCompletoDTO>> atualizarProfessor(
            @PathVariable Long id, @RequestBody PerfilAtualizacaoDTO dto) {
        
        ownershipService.validarAcessoDocente(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + id));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome().trim());
        }
        if (dto.getBioEfetiva() != null) {
            usuario.setBioProfessor(dto.getBioEfetiva().trim());
        }
        if (dto.getDataNascimentoEfetiva() != null && !dto.getDataNascimentoEfetiva().isBlank()) {
            usuario.setDataNascimento(dto.getDataNascimentoEfetiva().trim());
        }
        if (dto.getFotoUrl() != null && !dto.getFotoUrl().isBlank()) {
            usuario.setFotoUrl(dto.getFotoUrl().trim());
        }
        if (dto.getOferecePresencial() != null) {
            usuario.setOferecePresencial(dto.getOferecePresencial());
        }
        if (dto.getRegiaoAtendimento() != null) {
            usuario.setRegiaoAtendimento(dto.getRegiaoAtendimento().trim());
        }
        if (dto.getRaioAtendimentoKm() != null) {
            usuario.setRaioAtendimentoKm(dto.getRaioAtendimentoKm());
        }
        usuarioRepository.save(usuario);

        // Atualização de Aptidões Docentes
        if (dto.getAptidoes() != null) {
            aptidaoRepository.deleteByUsuarioId(id);
            for (var item : dto.getAptidoes()) {
                String nomeMateria = item.getMateria();
                if (nomeMateria == null || nomeMateria.isBlank()) continue;
                String materiaLimpa = nomeMateria.trim();

                Saber saber = saberRepository.findByNome(materiaLimpa)
                        .orElseGet(() -> {
                            Saber novo = new Saber();
                            novo.setNome(materiaLimpa);
                            novo.setCategoria("Geral");
                            return saberRepository.save(novo);
                        });

                Aptidao aptidao = new Aptidao();
                aptidao.setUsuario(usuario);
                aptidao.setSaber(saber);
                aptidao.setNivelDominio(item.getNivelDominio() != null ? item.getNivelDominio() : "Avançado");
                aptidao.setPrecoHora(item.getPreco() != null ? item.getPreco() : java.math.BigDecimal.valueOf(60.00));
                aptidaoRepository.save(aptidao);
            }
        }

        // Atualização de Grade Semanal de Disponibilidades
        if (dto.getDisponibilidades() != null) {
            disponibilidadeRepository.deleteByUsuarioId(id);
            for (var dispDto : dto.getDisponibilidades()) {
                if (dispDto.getDiaSemana() == null) continue;
                Disponibilidade disp = new Disponibilidade();
                disp.setUsuario(usuario);
                disp.setDiaSemana(dispDto.getDiaSemana().toUpperCase());
                if (dispDto.getHoraInicio() != null) {
                    disp.setHoraInicio(LocalTime.parse(dispDto.getHoraInicio()));
                }
                if (dispDto.getHoraFim() != null) {
                    disp.setHoraFim(LocalTime.parse(dispDto.getHoraFim()));
                }
                disponibilidadeRepository.save(disp);
            }
        }

        List<Interesse> interesses = interesseRepository.findByUsuarioId(id);
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(id);
        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByUsuarioId(id);

        PerfilCompletoDTO completo = PerfilCompletoDTO.fromEntities(usuario, interesses, aptidoes, disponibilidades);
        return ResponseEntity.ok(ApiResponse.ok(completo, "Perfil do professor atualizado com sucesso!"));
    }
}