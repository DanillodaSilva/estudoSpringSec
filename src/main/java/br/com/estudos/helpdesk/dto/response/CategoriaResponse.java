package br.com.estudos.helpdesk.dto.response;

import java.time.Instant;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        boolean ativo,
        Instant dataCriacao,
        Instant dataAtualizacao) {
}
