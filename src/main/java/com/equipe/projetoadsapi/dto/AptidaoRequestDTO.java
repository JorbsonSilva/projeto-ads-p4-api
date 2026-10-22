package com.equipe.projetoadsapi.dto;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO DE REQUISIÇÃO PARA APTIDÕES
 * * Defesa para a Banca: "Usamos DTOs para desacoplar a nossa visão (Front-end) do 
 * nosso modelo de banco de dados (Entidades). Este objeto captura o nome da matéria, 
 * o nível e o preço hora estipulado pelo professor no aplicativo."
 */
@Data
public class AptidaoRequestDTO {
    private String nomeSaber;
    private String nivelDominio;
    private BigDecimal precoHora;

    public String getMateria() {
        return nomeSaber;
    }

    public void setMateria(String materia) {
        this.nomeSaber = materia;
    }

    public BigDecimal getPreco() {
        return precoHora;
    }

    public void setPreco(BigDecimal preco) {
        this.precoHora = preco;
    }
}