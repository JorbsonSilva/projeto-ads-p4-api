package com.equipe.projetoadsapi.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.repository.SaberRepository;

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
}