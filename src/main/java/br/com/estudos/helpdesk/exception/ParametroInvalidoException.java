package br.com.estudos.helpdesk.exception;

/**
 * Parâmetro de consulta inválido, como uma propriedade de ordenação inexistente (HTTP 400).
 */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensagem) {
        super(mensagem);
    }
}
