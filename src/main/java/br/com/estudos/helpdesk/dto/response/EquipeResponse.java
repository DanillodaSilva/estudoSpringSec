package br.com.estudos.helpdesk.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record EquipeResponse(
        Long id,
        String nome,
        String descricao,
        boolean ativo,
        List<ResumoResponse> membros,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao) {
}
