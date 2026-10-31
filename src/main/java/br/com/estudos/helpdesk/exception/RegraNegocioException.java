package br.com.estudos.helpdesk.exception;

/**
 * Requisição bem formada que viola uma regra de negócio (HTTP 422).
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
