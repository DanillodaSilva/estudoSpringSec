package br.com.estudos.helpdesk.dto.response;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        boolean ativo,
        ResumoResponse equipe,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
