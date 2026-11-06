package com.equipe.projetoadsapi.model;

import com.equipe.projetoadsapi.model.enums.StatusSaque;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;

/**
 * Entidade que registra uma solicitação de saque financeiro efetuada por um professor.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Entity
@Table(name = "tb_saque")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Saque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professor_id", nullable = false)
    private Usuario professor;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "taxa_aplicada", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxaAplicada = BigDecimal.ZERO;

    @Column(name = "valor_liquido", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorLiquido;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_saque", nullable = false, length = 30)
    @Builder.Default
    private StatusSaque statusSaque = StatusSaque.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_chave_pix", length = 30)
    private TipoChavePix tipoChavePix;

    @Column(name = "chave_pix", length = 255)
    private String chavePix;

    @Column(name = "dados_bancarios", columnDefinition = "TEXT")
    private String dadosBancarios;

    @Column(name = "protocolo_saque", nullable = false, unique = true, length = 50)
    private String protocoloSaque;

    @Column(name = "data_solicitacao", nullable = false)
    private Instant dataSolicitacao;

    @Column(name = "data_processamento")
    private Instant dataProcessamento;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @PrePersist
    public void prePersist() {
        if (dataSolicitacao == null) {
            dataSolicitacao = Instant.now();
        }
        if (statusSaque == null) {
            statusSaque = StatusSaque.PENDENTE;
        }
        if (taxaAplicada == null) {
            taxaAplicada = BigDecimal.ZERO;
        }
        if (valorLiquido == null && valor != null) {
            valorLiquido = valor.subtract(taxaAplicada);
        }
        if (protocoloSaque == null || protocoloSaque.trim().isEmpty()) {
            int ano = Year.now().getValue();
            String sufixo = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            protocoloSaque = String.format("SAQ-%d-%s", ano, sufixo);
        }
    }
}
