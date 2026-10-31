package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.ComentarioAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ComentarioRequest;
import br.com.estudos.helpdesk.dto.response.ComentarioResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Comentario;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.mapper.ComentarioMapper;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.ComentarioRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comentários pertencem a um chamado e só podem ser criados, editados ou excluídos enquanto
 * ele não estiver FECHADO. Depois do fechamento, passam a fazer parte do registro definitivo.
 */
@Service
@Transactional(readOnly = true)
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ComentarioMapper mapper;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             ChamadoRepository chamadoRepository,
                             UsuarioRepository usuarioRepository,
                             ComentarioMapper mapper) {
        this.comentarioRepository = comentarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    public PaginaResponse<ComentarioResponse> listar(Long chamadoId, Pageable pageable) {
        if (!chamadoRepository.existsById(chamadoId)) {
            throw new RecursoNaoEncontradoException("Chamado", chamadoId);
        }
        return PaginaResponse.de(comentarioRepository.findByChamadoId(chamadoId, pageable), mapper::paraResponse);
    }

    public ComentarioResponse buscarPorId(Long chamadoId, Long comentarioId) {
        return mapper.paraResponse(buscarEntidade(chamadoId, comentarioId));
    }

    @Transactional
    public ComentarioResponse criar(Long chamadoId, ComentarioRequest request) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado", chamadoId));
        if (chamado.isFechado()) {
            throw new RegraNegocioException("Não é possível comentar em um chamado fechado.");
        }

        Usuario autor = usuarioRepository.findById(request.autorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", request.autorId()));
        if (!autor.isAtivo()) {
            throw new RegraNegocioException("O autor do comentário está inativo.");
        }

        return mapper.paraResponse(comentarioRepository.save(mapper.paraEntidade(request, autor, chamado)));
    }

    @Transactional
    public ComentarioResponse atualizar(Long chamadoId, Long comentarioId, ComentarioAtualizacaoRequest request) {
        Comentario comentario = buscarEntidade(chamadoId, comentarioId);
        if (comentario.getChamado().isFechado()) {
            throw new RegraNegocioException("Comentários de chamados fechados não podem ser editados.");
        }
        comentario.setConteudo(request.conteudo());
        comentarioRepository.flush();
        return mapper.paraResponse(comentario);
    }

    @Transactional
    public void excluir(Long chamadoId, Long comentarioId) {
        Comentario comentario = buscarEntidade(chamadoId, comentarioId);
        if (comentario.getChamado().isFechado()) {
            throw new RegraNegocioException("Comentários de chamados fechados não podem ser excluídos.");
        }
        comentarioRepository.delete(comentario);
    }

    private Comentario buscarEntidade(Long chamadoId, Long comentarioId) {
        return comentarioRepository.findByIdAndChamadoId(comentarioId, chamadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário", comentarioId));
    }
}
