package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.service.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List; // <-- Importação adicionada para trabalhar com listas
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<Agendamento> criarAgendamento(@RequestBody Agendamento agendamento) {
        try {
            Agendamento novo = agendamentoService.agendarAula(agendamento);
            return ResponseEntity.ok(novo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // <-- Nova rota GET adicionada para buscar as aulas do aluno
    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<Agendamento>> listarPorAluno(@PathVariable Long alunoId) {
        return ResponseEntity.ok(agendamentoService.buscarPorAluno(alunoId));
    }

    @PutMapping("/{id}/remarcar")
    public ResponseEntity<Agendamento> remarcarAgendamento(
            @PathVariable Long id, 
            @RequestBody Agendamento dadosAtualizados) {
        try {
            // Agora passamos o objeto 'dadosAtualizados' inteiro
            Agendamento atualizado = agendamentoService.remarcarAgendamento(id, dadosAtualizados);
            return ResponseEntity.ok(atualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

@PutMapping("/{id}/cancelar")
    public ResponseEntity<Agendamento> cancelarAgendamento(
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
        try {
            // Pega o texto da justificativa que veio do aplicativo
            String justificativa = payload.get("justificativa");
            return ResponseEntity.ok(agendamentoService.cancelarAgendamento(id, justificativa));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

// Rota GET chamada pela aba Agenda
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Agendamento>> listarPorProfessor(@PathVariable Long professorId) {
        return ResponseEntity.ok(agendamentoService.buscarPorProfessor(professorId));
    }

    // Rota PUT chamada pelo botão "Aceitar"
    @PutMapping("/{id}/aceitar")
    public ResponseEntity<Agendamento> aceitarAgendamento(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(agendamentoService.aceitarAgendamento(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}