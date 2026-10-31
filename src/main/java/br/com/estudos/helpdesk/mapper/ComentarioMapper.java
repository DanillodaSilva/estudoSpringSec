package br.com.estudos.helpdesk.mapper;

import br.com.estudos.helpdesk.dto.request.ComentarioRequest;
import br.com.estudos.helpdesk.dto.response.ComentarioResponse;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Comentario;
import br.com.estudos.helpdesk.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ComentarioMapper {

    public Comentario paraEntidade(ComentarioRequest request, Usuario autor, Chamado chamado) {
        return new Comentario(request.conteudo(), autor, chamado);
    }

    public ComentarioResponse paraResponse(Comentario comentario) {
        Usuario autor = comentario.getAutor();
        return new ComentarioResponse(
                comentario.getId(),
                comentario.getChamado().getId(),
                new ResumoResponse(autor.getId(), autor.getNome()),
                comentario.getConteudo(),
                comentario.getDataCriacao(),
                comentario.getDataAtualizacao());
    }
}
