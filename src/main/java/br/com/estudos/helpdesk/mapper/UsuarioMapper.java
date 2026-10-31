package br.com.estudos.helpdesk.mapper;

import br.com.estudos.helpdesk.dto.request.UsuarioRequest;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.dto.response.UsuarioResponse;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario paraEntidade(UsuarioRequest request, Equipe equipe) {
        Usuario usuario = new Usuario(request.nome(), request.email(), equipe);
        if (request.ativo() != null) {
            usuario.setAtivo(request.ativo());
        }
        return usuario;
    }

    public void atualizarEntidade(Usuario usuario, UsuarioRequest request, Equipe equipe) {
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setEquipe(equipe);
        if (request.ativo() != null) {
            usuario.setAtivo(request.ativo());
        }
    }

    public UsuarioResponse paraResponse(Usuario usuario) {
        Equipe equipe = usuario.getEquipe();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.isAtivo(),
                equipe == null ? null : new ResumoResponse(equipe.getId(), equipe.getNome()),
                usuario.getDataCriacao(),
                usuario.getDataAtualizacao());
    }
}
