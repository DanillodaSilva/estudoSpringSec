package br.com.estudos.helpdesk.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Atribui o chamado a uma equipe e, opcionalmente, a um responsável que pertença a ela.
 *
 * @param responsavelId opcional; nulo deixa o chamado apenas com a equipe (remove o responsável atual).
 */
public record AtribuicaoRequest(
        @NotNull(message = "O id da equipe é obrigatório")
        @Positive(message = "O id da equipe deve ser positivo")
        Long equipeId,

        @Positive(message = "O id do responsável deve ser positivo")
        Long responsavelId) {
}
