package br.com.estudos.helpdesk.dto.request;

import br.com.estudos.helpdesk.model.enums.StatusChamado;
import jakarta.validation.constraints.NotNull;

public record AlteracaoStatusRequest(
        @NotNull(message = "O novo status é obrigatório")
        StatusChamado status) {
}
