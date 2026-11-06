package com.equipe.projetoadsapi.model;

import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidade que audita movimentações financeiras individuais (livro-razão).
 * Rastreabilidade: Fase 7 - Bloco J (BACK-009)
 */
@Entity
@Table(name = "tb_transacao_financeira")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saque_id")
    private Saque saque;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transacao", nullable = false, length = 50)
    private TipoTransacao tipoTransacao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(length = 255)
    private String descricao;

    @Column(name = "data_transacao", nullable = false)
    private Instant dataTransacao;

    @PrePersist
    public void prePersist() {
        if (dataTransacao == null) {
            dataTransacao = Instant.now();
        }
    }
}
