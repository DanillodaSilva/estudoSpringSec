package br.com.estudos.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param ativo opcional; na criação o padrão é {@code true} e na atualização o valor atual é mantido.
 */
public record CategoriaRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        Boolean ativo) {

    public CategoriaRequest {
        nome = nome == null ? null : nome.strip();
    }
}
