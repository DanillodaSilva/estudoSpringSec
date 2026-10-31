package br.com.estudos.helpdesk.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * @param equipeId opcional; nulo significa usuário sem equipe.
 * @param ativo    opcional; na criação o padrão é {@code true} e na atualização o valor atual é mantido.
 */
public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ser válido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @Positive(message = "O id da equipe deve ser positivo")
        Long equipeId,

        Boolean ativo) {

    public UsuarioRequest {
        nome = nome == null ? null : nome.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
