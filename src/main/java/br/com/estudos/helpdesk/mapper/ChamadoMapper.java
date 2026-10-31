package br.com.estudos.helpdesk.mapper;

import br.com.estudos.helpdesk.dto.request.ChamadoAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoRequest;
import br.com.estudos.helpdesk.dto.response.ChamadoResponse;
import br.com.estudos.helpdesk.dto.response.HistoricoStatusResponse;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.HistoricoStatus;
import br.com.estudos.helpdesk.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class ChamadoMapper {

    public Chamado paraEntidade(ChamadoRequest request, Usuario solicitante, Categoria categoria) {
        return new Chamado(request.titulo(), request.descricao(), request.prioridade(), solicitante, categoria);
    }

    public void atualizarEntidade(Chamado chamado, ChamadoAtualizacaoRequest request, Categoria categoria) {
        chamado.setTitulo(request.titulo());
        chamado.setDescricao(request.descricao());
        chamado.setPrioridade(request.prioridade());
        chamado.setCategoria(categoria);
    }

    public ChamadoResponse paraResponse(Chamado chamado) {
        return new ChamadoResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                resumo(chamado.getSolicitante()),
                resumo(chamado.getResponsavel()),
                resumo(chamado.getEquipe()),
                resumo(chamado.getCategoria()),
                chamado.getDataAbertura(),
                chamado.getDataAtualizacao(),
                chamado.getDataEncerramento());
    }

    public HistoricoStatusResponse paraResponse(HistoricoStatus historico) {
        return new HistoricoStatusResponse(
                historico.getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getDataAlteracao());
    }

    private static ResumoResponse resumo(Usuario usuario) {
        return usuario == null ? null : new ResumoResponse(usuario.getId(), usuario.getNome());
    }

    private static ResumoResponse resumo(Equipe equipe) {
        return equipe == null ? null : new ResumoResponse(equipe.getId(), equipe.getNome());
    }

    private static ResumoResponse resumo(Categoria categoria) {
        return categoria == null ? null : new ResumoResponse(categoria.getId(), categoria.getNome());
    }
}
