package br.com.estudos.helpdesk.dto.request;

import br.com.estudos.helpdesk.model.enums.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Abertura de chamado.
 *
 * @param solicitanteId informado pelo próprio cliente APENAS nesta versão de estudo, para exercitar as
 *                      regras de domínio. Não é prova de identidade: qualquer cliente pode enviar qualquer id.
 */
public record ChamadoRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,

        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
        String descricao,

        @NotNull(message = "A prioridade é obrigatória")
        Prioridade prioridade,

        @NotNull(message = "O id do solicitante é obrigatório")
        @Positive(message = "O id do solicitante deve ser positivo")
        Long solicitanteId,

        @NotNull(message = "O id da categoria é obrigatório")
        @Positive(message = "O id da categoria deve ser positivo")
        Long categoriaId) {

    public ChamadoRequest {
        titulo = titulo == null ? null : titulo.strip();
        descricao = descricao == null ? null : descricao.strip();
    }
}
