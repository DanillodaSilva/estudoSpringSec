package br.com.estudos.helpdesk.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pessoa cadastrada na organização. Nesta etapa é apenas uma entidade de domínio:
 * não possui credenciais, perfis ou qualquer dado de autenticação.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    @Column(nullable = false, updatable = false)
    private Instant dataCriacao;

    @Column(nullable = false)
    private Instant dataAtualizacao;

    protected Usuario() {
    }

    public Usuario(String nome, String email, Equipe equipe) {
        this.nome = nome;
        this.email = email;
        this.equipe = equipe;
    }

    @PrePersist
    void aoCriar() {
        dataCriacao = Instant.now().truncatedTo(ChronoUnit.MICROS);
        dataAtualizacao = dataCriacao;
    }

    @PreUpdate
    void aoAtualizar() {
        dataAtualizacao = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public boolean pertenceA(Equipe outraEquipe) {
        return equipe != null && outraEquipe != null && equipe.getId() != null
                && equipe.getId().equals(outraEquipe.getId());
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }
}
