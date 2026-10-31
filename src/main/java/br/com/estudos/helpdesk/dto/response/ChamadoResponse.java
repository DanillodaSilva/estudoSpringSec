package br.com.estudos.helpdesk.dto.response;

import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;

import java.time.Instant;

public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        Prioridade prioridade,
        StatusChamado status,
        ResumoResponse solicitante,
        ResumoResponse responsavel,
        ResumoResponse equipe,
        ResumoResponse categoria,
        Instant dataAbertura,
        Instant dataAtualizacao,
        Instant dataEncerramento) {
}
