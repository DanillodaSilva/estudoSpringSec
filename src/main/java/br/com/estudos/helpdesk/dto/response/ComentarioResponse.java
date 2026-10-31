package br.com.estudos.helpdesk.dto.response;

import java.time.Instant;

public record ComentarioResponse(
        Long id,
        Long chamadoId,
        ResumoResponse autor,
        String conteudo,
        Instant dataCriacao,
        Instant dataAtualizacao) {
}
