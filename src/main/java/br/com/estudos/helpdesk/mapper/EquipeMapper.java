package br.com.estudos.helpdesk.mapper;

import br.com.estudos.helpdesk.dto.request.EquipeRequest;
import br.com.estudos.helpdesk.dto.response.EquipeResponse;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.entity.Equipe;
import org.springframework.stereotype.Component;

@Component
public class EquipeMapper {

    public Equipe paraEntidade(EquipeRequest request) {
        Equipe equipe = new Equipe(request.nome(), request.descricao());
        if (request.ativo() != null) {
            equipe.setAtivo(request.ativo());
        }
        return equipe;
    }

    public void atualizarEntidade(Equipe equipe, EquipeRequest request) {
        equipe.setNome(request.nome());
        equipe.setDescricao(request.descricao());
        if (request.ativo() != null) {
            equipe.setAtivo(request.ativo());
        }
    }

    public EquipeResponse paraResponse(Equipe equipe) {
        return new EquipeResponse(
                equipe.getId(),
                equipe.getNome(),
                equipe.getDescricao(),
                equipe.isAtivo(),
                equipe.getMembros().stream()
                        .map(membro -> new ResumoResponse(membro.getId(), membro.getNome()))
                        .toList(),
                equipe.getDataCriacao(),
                equipe.getDataAtualizacao());
    }
}
