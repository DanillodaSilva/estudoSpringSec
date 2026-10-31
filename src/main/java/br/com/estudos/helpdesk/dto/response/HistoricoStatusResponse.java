package br.com.estudos.helpdesk.dto.response;

import br.com.estudos.helpdesk.model.enums.StatusChamado;

import java.time.Instant;

public record HistoricoStatusResponse(
        Long id,
        StatusChamado statusAnterior,
        StatusChamado statusNovo,
        Instant dataAlteracao) {
}
