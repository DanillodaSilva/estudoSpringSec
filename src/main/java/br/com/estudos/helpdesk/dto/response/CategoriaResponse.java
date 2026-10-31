package br.com.estudos.helpdesk.dto.response;

import java.time.LocalDateTime;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        boolean ativo,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
