package br.com.estudos.helpdesk.dto.response;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        boolean ativo,
        ResumoResponse equipe,
        Instant dataCriacao,
        Instant dataAtualizacao) {
}
