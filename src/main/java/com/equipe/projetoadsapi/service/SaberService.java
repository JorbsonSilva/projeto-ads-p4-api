package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.CategoriaPopularDTO;
import com.equipe.projetoadsapi.model.PapelUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.repository.SaberRepository;

import java.util.List;

/**
 * SERVIÇO DE SABERES (Catálogo)
 * * Roteiro de Defesa: "Isolamos a lógica de consulta do catálogo em um Service 
 * dedicado para manter a arquitetura limpa e facilitar futuras implementações, 
 * como paginação ou filtros por categoria."
 */
@Service
public class SaberService {

    @Autowired
    private SaberRepository repository;

    public List<Saber> listarTodos() {
        return repository.findAll(); // Puxa todas as matérias cadastradas no Seeding
    }

    public List<CategoriaPopularDTO> listarCategoriasPopulares() {
        return repository.findCategoriasMaisEnsinadas(PapelUsuario.ROLE_PROFESSOR, PageRequest.of(0, 10));
    }
}