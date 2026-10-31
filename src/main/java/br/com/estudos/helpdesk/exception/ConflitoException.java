package br.com.estudos.helpdesk.exception;

/**
 * Operação em conflito com o estado atual dos dados, como e-mail duplicado ou
 * exclusão de registro que possui vínculos (HTTP 409).
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
