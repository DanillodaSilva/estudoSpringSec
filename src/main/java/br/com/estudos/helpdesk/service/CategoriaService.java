package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.CategoriaRequest;
import br.com.estudos.helpdesk.dto.response.CategoriaResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.mapper.CategoriaMapper;
import br.com.estudos.helpdesk.repository.CategoriaRepository;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ChamadoRepository chamadoRepository;
    private final CategoriaMapper mapper;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            ChamadoRepository chamadoRepository,
                            CategoriaMapper mapper) {
        this.categoriaRepository = categoriaRepository;
        this.chamadoRepository = chamadoRepository;
        this.mapper = mapper;
    }

    public PaginaResponse<CategoriaResponse> listar(Pageable pageable) {
        return PaginaResponse.de(categoriaRepository.findAll(pageable), mapper::paraResponse);
    }

    public CategoriaResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNome(request.nome())) {
            throw new ConflitoException("Já existe uma categoria com o nome '%s'.".formatted(request.nome()));
        }
        return mapper.paraResponse(categoriaRepository.save(mapper.paraEntidade(request)));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidade(id);
        if (categoriaRepository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new ConflitoException("Já existe uma categoria com o nome '%s'.".formatted(request.nome()));
        }
        mapper.atualizarEntidade(categoria, request);
        // flush para que a data de atualização (@PreUpdate) já apareça na resposta
        categoriaRepository.flush();
        return mapper.paraResponse(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        if (chamadoRepository.existsByCategoriaId(id)) {
            throw new ConflitoException(
                    "A categoria possui chamados vinculados e não pode ser excluída; desative-a.");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }
}
