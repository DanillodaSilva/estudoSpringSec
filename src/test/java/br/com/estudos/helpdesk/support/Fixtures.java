package br.com.estudos.helpdesk.support;

import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Comentario;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Entidades com id definido, para testes unitários sem banco de dados.
 */
public final class Fixtures {

    private Fixtures() {
    }

    public static Equipe equipe(Long id, boolean ativo) {
        Equipe equipe = new Equipe("Equipe " + id, "Equipe de teste");
        equipe.setAtivo(ativo);
        return comId(equipe, id);
    }

    public static Categoria categoria(Long id, boolean ativo) {
        Categoria categoria = new Categoria("Categoria " + id, "Categoria de teste");
        categoria.setAtivo(ativo);
        return comId(categoria, id);
    }

    public static Usuario usuario(Long id, boolean ativo, Equipe equipe) {
        Usuario usuario = new Usuario("Usuário " + id, "usuario" + id + "@example.com", equipe);
        usuario.setAtivo(ativo);
        return comId(usuario, id);
    }

    public static Chamado chamado(Long id, StatusChamado status, Usuario solicitante, Categoria categoria) {
        Chamado chamado = new Chamado("Chamado " + id, "Descrição do chamado", Prioridade.MEDIA, solicitante, categoria);
        chamado.setStatus(status);
        return comId(chamado, id);
    }

    public static Comentario comentario(Long id, Usuario autor, Chamado chamado) {
        return comId(new Comentario("Comentário " + id, autor, chamado), id);
    }

    private static <T> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
