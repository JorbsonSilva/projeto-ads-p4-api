package com.equipe.projetoadsapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_usuario_papeis", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "papel", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<PapelUsuario> papeis = new HashSet<>();

    @Builder.Default
    @Column(name = "email_verificado")
    private Boolean emailVerificado = false;

    @Column(name = "cpf", unique = true, length = 14)
    private String cpf;

    @Column(name = "data_nascimento", length = 10)
    private String dataNascimento;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "estado", length = 2)
    private String estado;

    @Column(name = "titulacao", length = 200)
    private String titulacao;

    @Column(name = "preco_hora_online", precision = 10, scale = 2)
    private BigDecimal precoHoraOnline;

    @Column(name = "preco_hora_presencial", precision = 10, scale = 2)
    private BigDecimal precoHoraPresencial;

    @Column(name = "chave_pix", length = 255)
    private String chavePix;

    @Column(name = "tipo_chave_pix", length = 30)
    private String tipoChavePix;

    @Column(name = "banco", length = 120)
    private String banco;

    @Column(name = "aceita_online")
    private Boolean aceitaOnline;

    @Column(name = "bio_aluno", columnDefinition = "TEXT")
    private String bioAluno;

    @Column(name = "bio_professor", columnDefinition = "TEXT")
    private String bioProfessor;

    @Column(name = "foto_url", length = 1000)
    private String fotoUrl;

    @Column(name = "nota_media", columnDefinition = "numeric")
    private Double notaMedia;

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
    @Column(name = "latitude_aproximada", columnDefinition = "numeric")
    private Double latitudeAproximada;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(name = "longitude_aproximada", columnDefinition = "numeric")
    private Double longitudeAproximada;

    @Builder.Default
    @Column(name = "raio_atendimento_km")
    private Integer raioAtendimentoKm = 10;

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

    public void setPapel(PapelUsuario papel) {
        this.papel = papel;
        adicionarPapel(papel);
    }

    public void adicionarPapel(PapelUsuario papel) {
        if (papel == null) return;
        if (papeis == null) papeis = new HashSet<>();
        papeis.add(papel);
    }

    public boolean possuiPapel(PapelUsuario papel) {
        return papel != null && papeis != null && papeis.contains(papel);
    }

    public void setTipo(String tipo) {
        if (tipo == null) return;
        String normalizado = tipo.toUpperCase();
        if (!normalizado.startsWith("ROLE_")) {
            normalizado = "ROLE_" + normalizado;
        }
        try {
            setPapel(PapelUsuario.valueOf(normalizado));
        } catch (IllegalArgumentException e) {
            setPapel(PapelUsuario.ROLE_ALUNO);
        }
    }

    @PrePersist
    @PreUpdate
    private void garantirPapelAtivoEntreOsPapeisDaConta() {
        adicionarPapel(papel);
    }
}