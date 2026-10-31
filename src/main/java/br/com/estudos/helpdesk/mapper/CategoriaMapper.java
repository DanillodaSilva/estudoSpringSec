package br.com.estudos.helpdesk.mapper;

import br.com.estudos.helpdesk.dto.request.CategoriaRequest;
import br.com.estudos.helpdesk.dto.response.CategoriaResponse;
import br.com.estudos.helpdesk.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria paraEntidade(CategoriaRequest request) {
        Categoria categoria = new Categoria(request.nome(), request.descricao());
        if (request.ativo() != null) {
            categoria.setAtivo(request.ativo());
        }
        return categoria;
    }

    public void atualizarEntidade(Categoria categoria, CategoriaRequest request) {
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        if (request.ativo() != null) {
            categoria.setAtivo(request.ativo());
        }
    }

    public CategoriaResponse paraResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao(),
                categoria.isAtivo(),
                categoria.getDataCriacao(),
                categoria.getDataAtualizacao());
    }
}
