package br.com.estudos.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @param autorId informado pelo próprio cliente APENAS nesta versão de estudo. Não é prova de
 *                identidade: qualquer cliente pode enviar qualquer id.
 */
public record ComentarioRequest(
        @NotNull(message = "O id do autor é obrigatório")
        @Positive(message = "O id do autor deve ser positivo")
        Long autorId,

        @NotBlank(message = "O conteúdo é obrigatório")
        @Size(max = 2000, message = "O conteúdo deve ter no máximo 2000 caracteres")
        String conteudo) {

    public ComentarioRequest {
        conteudo = conteudo == null ? null : conteudo.strip();
    }
}
