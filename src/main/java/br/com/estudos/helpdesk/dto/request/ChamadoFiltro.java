package br.com.estudos.helpdesk.dto.request;

import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;

/**
 * Filtros opcionais da listagem de chamados, recebidos como query params
 * (ex.: {@code ?status=ABERTO&prioridade=ALTA&responsavelId=3}).
 */
public record ChamadoFiltro(
        StatusChamado status,
        Prioridade prioridade,
        Long categoriaId,
        Long responsavelId,
        Long equipeId,
        Long solicitanteId) {
}
