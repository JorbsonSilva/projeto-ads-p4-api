package com.equipe.projetoadsapi.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO UNIFICADO PARA ATUALIZAÇÃO DE PERFIL
 * * Defesa para a Banca: "Este DTO flexível encapsula qualquer dado vindo da tela de 
 * perfil do aplicativo. Ele suporta tanto os campos textuais básicos (nome, nascimento, bio) 
 * quanto as listas dinâmicas de interesses ou aptidões, dependendo do papel do usuário."
 */
@Data
public class PerfilAtualizacaoDTO {
    private String nome;
    private String nascimento;
    private String bio;
    
    // Lista usada se o usuário estiver salvando como Aluno
    private List<String> interessesNomes;
    
    // Lista usada se o usuário estiver salvando como Professor
    private List<AptidaoRequestDTO> aptidoes;
}