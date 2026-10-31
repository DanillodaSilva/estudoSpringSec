package br.com.estudos.helpdesk.entity;

import br.com.estudos.helpdesk.model.enums.StatusChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro imutável de uma mudança de status. Ainda não identifica quem fez a alteração:
 * esse vínculo depende da identidade autenticada, que será introduzida futuramente.
 */
@Entity
@Immutable
@Table(name = "historico_status_chamados")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false, updatable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private StatusChamado statusNovo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    protected HistoricoStatus() {
    }

    public HistoricoStatus(Chamado chamado, StatusChamado statusAnterior, StatusChamado statusNovo) {
        this.chamado = chamado;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
    }

    @PrePersist
    void aoCriar() {
        dataAlteracao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Chamado getChamado() {
        return chamado;
    }

    public StatusChamado getStatusAnterior() {
        return statusAnterior;
    }

    public StatusChamado getStatusNovo() {
        return statusNovo;
    }

    public LocalDateTime getDataAlteracao() {
        return dataAlteracao;
    }
}
