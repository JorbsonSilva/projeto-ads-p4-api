package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

/**
 * ENTIDADE USUÁRIO (Mapeamento Objeto-Relacional)
 * * Roteiro de Defesa para a Banca: "Utilizamos a especificação JPA com o framework Hibernate 
 * para fazer o mapeamento da nossa classe Java diretamente para a tabela 'tb_usuarios' do Supabase. 
 * Adicionamos restrições como 'unique = true' nos campos de E-mail e CPF para garantir a integridade 
 * dos dados no nível do banco, impedindo cadastros duplicados de forma nativa."
 */
@Entity // Diz que é uma tabela do banco
@Table(name = "tb_usuarios") // Dá o nome da tabela
@Getter // Cria os gets de tudo (Lombok)
@Setter // Cria os sets de tudo (Lombok)
@NoArgsConstructor // Construtor vazio para o banco de dados
@AllArgsConstructor
@Builder // Construtor cheio para você usar no código
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    // 🟢 DICA DE SEGURANÇA: Faz a senha ser aceita ao salvar, mas nunca enviada de volta pro celular
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "senha", nullable = false)
    private String senha;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", length = 30)
    private PapelUsuario papel = PapelUsuario.ROLE_ALUNO;

    @Builder.Default
    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @Column(name = "cpf", unique = true, length = 14)
    private String cpf;

    @Column(name = "data_nascimento", length = 10)
    private String dataNascimento;

    @Column(name = "bio_aluno", columnDefinition = "TEXT")
    private String bioAluno;

    @Column(name = "bio_professor", columnDefinition = "TEXT")
    private String bioProfessor;

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

    @Builder.Default
    @Column(name = "nota_media", columnDefinition = "numeric")
    private Double notaMedia = 5.0; // Professor recém-cadastrado começa com 5 estrelas

    @Builder.Default
    @Column(name = "total_avaliacoes")
    private Integer totalAvaliacoes = 0; // Começa com 0 avaliações

    // =========================================================================
    // LOCALIZAÇÃO E ATENDIMENTO PRESENCIAL (Fase 8 / MAP-001, MAP-002, LGPD)
    // =========================================================================
    @Builder.Default
    @Column(name = "oferece_presencial")
    private Boolean oferecePresencial = false;

    @Column(name = "regiao_atendimento", length = 100)
    private String regiaoAtendimento;

    // Coordenadas protegidas no servidor - não expostas ao cliente (MAP-SEC-002)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "latitude_aproximada")
    private Double latitudeAproximada;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "longitude_aproximada")
    private Double longitudeAproximada;

    @Builder.Default
    @Column(name = "raio_atendimento_km")
    private Integer raioAtendimentoKm = 10;

    // =========================================================================
    // DADOS FINANCEIROS DEMONSTRATIVOS (Fase 5 / Decisão Técnica 18 - PROP-FIN-01)
    // Sem cobrança real, sem gateways pagos e estritamente proibido salvar CVV
    // =========================================================================
    @Column(name = "cartao_ficticio_bandeira", length = 30)
    private String cartaoFicticioBandeira;

    @Column(name = "cartao_ficticio_ultimos_digitos", length = 4)
    private String cartaoFicticioUltimosDigitos;

    @Column(name = "cartao_ficticio_titular", length = 100)
    private String cartaoFicticioTitular;

    // 🟢 CORREÇÃO: Envia a lista para o Front-end, mas quebra o loop infinito ignorando apenas o 'usuario' do outro lado
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Interesse> interesses;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Aptidao> aptidoes;

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("usuario")
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<Disponibilidade> disponibilidades;

    public String getTipo() {
        return papel != null ? papel.name().replace("ROLE_", "") : "ALUNO";
    }

    public void setTipo(String tipo) {
        if (tipo == null) return;
        String normalizado = tipo.toUpperCase();
        if (!normalizado.startsWith("ROLE_")) {
            normalizado = "ROLE_" + normalizado;
        }
        try {
            this.papel = PapelUsuario.valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            this.papel = PapelUsuario.ROLE_ALUNO;
        }
    }
}