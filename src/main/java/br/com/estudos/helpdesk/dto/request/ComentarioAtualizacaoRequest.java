package br.com.estudos.helpdesk.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Apenas o conteúdo é editável; autor e chamado são definidos na criação.
 */
public record ComentarioAtualizacaoRequest(
        @NotBlank(message = "O conteúdo é obrigatório")
        @Size(max = 2000, message = "O conteúdo deve ter no máximo 2000 caracteres")
        String conteudo) {

    public ComentarioAtualizacaoRequest {
        conteudo = conteudo == null ? null : conteudo.strip();
    }
}
