package br.com.estudos.helpdesk.dto.response;

/**
 * Referência compacta a outro recurso (usuário, equipe ou categoria) dentro de uma resposta.
 */
public record ResumoResponse(Long id, String nome) {
}
