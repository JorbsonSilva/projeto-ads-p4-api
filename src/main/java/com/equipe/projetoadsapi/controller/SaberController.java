package com.equipe.projetoadsapi.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.service.SaberService;

/**
 * CONTROLLER DE SABERES
 * * Roteiro de Defesa: "Disponibilizamos um endpoint GET simples e rápido. 
 * A anotação @CrossOrigin permite que o aplicativo React Native consuma a API 
 * sem ser bloqueado pelas políticas de segurança do navegador (CORS)."
 */
@RestController
@RequestMapping("/api/saberes")
@CrossOrigin(origins = "*")
public class SaberController {

    @Autowired
    private SaberService service;

    @GetMapping
    public ResponseEntity<List<Saber>> listarSaberes() {
        // Devolve o JSON com a lista completa para o celular
        return ResponseEntity.ok(service.listarTodos());
    }
}