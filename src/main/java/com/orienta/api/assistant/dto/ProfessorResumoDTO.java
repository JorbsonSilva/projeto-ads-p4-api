package com.orienta.api.assistant.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resumo seguro e descaracterizado de informações públicas do professor retornado na triagem.
 * Não expõe dados sensíveis (LGPD: endereço exato, telefone pessoal ou e-mail ocultos).
 */
public class ProfessorResumoDTO implements Serializable {
    private Long id;
    private String nome;
    private String tituloAcademico;
    private BigDecimal valorHora;
    private Double avaliacaoMedia;
    private Integer totalAvaliacoes;
    private List<String> modalidades;
    private String regiaoBairro;
    private List<String> disciplinas;
    private Boolean disponivelEstaSemana;

    public ProfessorResumoDTO() {
    }

    public ProfessorResumoDTO(Long id, String nome, String tituloAcademico, BigDecimal valorHora,
                              Double avaliacaoMedia, Integer totalAvaliacoes, List<String> modalidades,
                              String regiaoBairro, List<String> disciplinas, Boolean disponivelEstaSemana) {
        this.id = id;
        this.nome = nome;
        this.tituloAcademico = tituloAcademico;
        this.valorHora = valorHora;
        this.avaliacaoMedia = avaliacaoMedia;
        this.totalAvaliacoes = totalAvaliacoes;
        this.modalidades = modalidades;
        this.regiaoBairro = regiaoBairro;
        this.disciplinas = disciplinas;
        this.disponivelEstaSemana = disponivelEstaSemana;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTituloAcademico() {
        return tituloAcademico;
    }

    public void setTituloAcademico(String tituloAcademico) {
        this.tituloAcademico = tituloAcademico;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal valorHora) {
        this.valorHora = valorHora;
    }

    public Double getAvaliacaoMedia() {
        return avaliacaoMedia;
    }

    public void setAvaliacaoMedia(Double avaliacaoMedia) {
        this.avaliacaoMedia = avaliacaoMedia;
    }

    public Integer getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public void setTotalAvaliacoes(Integer totalAvaliacoes) {
        this.totalAvaliacoes = totalAvaliacoes;
    }

    public List<String> getModalidades() {
        return modalidades;
    }

    public void setModalidades(List<String> modalidades) {
        this.modalidades = modalidades;
    }

    public String getRegiaoBairro() {
        return regiaoBairro;
    }

    public void setRegiaoBairro(String regiaoBairro) {
        this.regiaoBairro = regiaoBairro;
    }

    public List<String> getDisciplinas() {
        return disciplinas;
    }

    public void setDisciplinas(List<String> disciplinas) {
        this.disciplinas = disciplinas;
    }

    public Boolean getDisponivelEstaSemana() {
        return disponivelEstaSemana;
    }

    public void setDisponivelEstaSemana(Boolean disponivelEstaSemana) {
        this.disponivelEstaSemana = disponivelEstaSemana;
    }
}
