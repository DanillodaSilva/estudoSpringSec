package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.AlteracaoStatusRequest;
import br.com.estudos.helpdesk.dto.request.AtribuicaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoFiltro;
import br.com.estudos.helpdesk.dto.request.ChamadoRequest;
import br.com.estudos.helpdesk.dto.response.ChamadoResponse;
import br.com.estudos.helpdesk.dto.response.HistoricoStatusResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.HistoricoStatus;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.ParametroInvalidoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.mapper.ChamadoMapper;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.CategoriaRepository;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.ComentarioRepository;
import br.com.estudos.helpdesk.repository.EquipeRepository;
import br.com.estudos.helpdesk.repository.HistoricoStatusRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class ChamadoService {

    private static final Set<String> CAMPOS_ORDENACAO = new TreeSet<>(Set.of(
            "id", "titulo", "prioridade", "status", "dataAbertura", "dataAtualizacao", "dataEncerramento"));

    private final ChamadoRepository chamadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipeRepository equipeRepository;
    private final CategoriaRepository categoriaRepository;
    private final HistoricoStatusRepository historicoRepository;
    private final ComentarioRepository comentarioRepository;
    private final ChamadoMapper mapper;

    public ChamadoService(ChamadoRepository chamadoRepository,
                          UsuarioRepository usuarioRepository,
                          EquipeRepository equipeRepository,
                          CategoriaRepository categoriaRepository,
                          HistoricoStatusRepository historicoRepository,
                          ComentarioRepository comentarioRepository,
                          ChamadoMapper mapper) {
        this.chamadoRepository = chamadoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipeRepository = equipeRepository;
        this.categoriaRepository = categoriaRepository;
        this.historicoRepository = historicoRepository;
        this.comentarioRepository = comentarioRepository;
        this.mapper = mapper;
    }

    public PaginaResponse<ChamadoResponse> listar(ChamadoFiltro filtro, Pageable pageable) {
        // A consulta é JPQL: uma propriedade inexistente no ?sort= só falharia dentro do Hibernate.
        pageable.getSort().forEach(ordem -> {
            if (!CAMPOS_ORDENACAO.contains(ordem.getProperty())) {
                throw new ParametroInvalidoException("Propriedade de ordenação inválida: '%s'. Permitidas: %s."
                        .formatted(ordem.getProperty(), CAMPOS_ORDENACAO));
            }
        });
        return PaginaResponse.de(
                chamadoRepository.filtrar(filtro.status(), filtro.prioridade(), filtro.categoriaId(),
                        filtro.responsavelId(), filtro.equipeId(), filtro.solicitanteId(), pageable),
                mapper::paraResponse);
    }

    public ChamadoResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public ChamadoResponse abrir(ChamadoRequest request) {
        Usuario solicitante = buscarUsuario(request.solicitanteId());
        if (!solicitante.isAtivo()) {
            throw new RegraNegocioException("O solicitante informado está inativo.");
        }
        Categoria categoria = buscarCategoriaAtiva(request.categoriaId());

        // O construtor de Chamado define o status inicial ABERTO.
        Chamado chamado = mapper.paraEntidade(request, solicitante, categoria);
        return mapper.paraResponse(chamadoRepository.save(chamado));
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoAtualizacaoRequest request) {
        Chamado chamado = buscarEntidade(id);
        garantirNaoFechado(chamado);

        Categoria categoria = chamado.getCategoria();
        if (!categoria.getId().equals(request.categoriaId())) {
            categoria = buscarCategoriaAtiva(request.categoriaId());
        }

        mapper.atualizarEntidade(chamado, request, categoria);
        chamadoRepository.flush();
        return mapper.paraResponse(chamado);
    }

    /**
     * Aplica uma transição de status conforme {@link StatusChamado} e registra o histórico.
     * Ao fechar, a data de encerramento é definida automaticamente.
     */
    @Transactional
    public ChamadoResponse alterarStatus(Long id, AlteracaoStatusRequest request) {
        Chamado chamado = buscarEntidade(id);
        garantirNaoFechado(chamado);

        StatusChamado atual = chamado.getStatus();
        StatusChamado novo = request.status();
        if (!atual.podeTransicionarPara(novo)) {
            throw new RegraNegocioException("Transição de status inválida: %s -> %s. Transições permitidas: %s."
                    .formatted(atual, novo, atual.transicoesPermitidas()));
        }

        chamado.setStatus(novo);
        if (novo == StatusChamado.FECHADO) {
            chamado.setDataEncerramento(Instant.now().truncatedTo(ChronoUnit.MICROS));
        }
        historicoRepository.save(new HistoricoStatus(chamado, atual, novo));
        chamadoRepository.flush();
        return mapper.paraResponse(chamado);
    }

    @Transactional
    public ChamadoResponse atribuir(Long id, AtribuicaoRequest request) {
        Chamado chamado = buscarEntidade(id);
        garantirNaoFechado(chamado);

        Equipe equipe = equipeRepository.findById(request.equipeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", request.equipeId()));
        if (!equipe.isAtivo()) {
            throw new RegraNegocioException("Não é possível atribuir chamados a uma equipe inativa.");
        }

        Usuario responsavel = null;
        if (request.responsavelId() != null) {
            responsavel = buscarUsuario(request.responsavelId());
            if (!responsavel.isAtivo()) {
                throw new RegraNegocioException("Não é possível atribuir chamados a um usuário inativo.");
            }
            if (!responsavel.pertenceA(equipe)) {
                throw new RegraNegocioException("O responsável precisa pertencer à equipe atribuída ao chamado.");
            }
        }

        chamado.setEquipe(equipe);
        chamado.setResponsavel(responsavel);
        chamadoRepository.flush();
        return mapper.paraResponse(chamado);
    }

    public List<HistoricoStatusResponse> listarHistorico(Long id) {
        if (!chamadoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Chamado", id);
        }
        return historicoRepository.findByChamadoIdOrderByDataAlteracaoAscIdAsc(id).stream()
                .map(mapper::paraResponse)
                .toList();
    }

    /**
     * Somente chamados sem movimentação (sem histórico de status e sem comentários) podem ser excluídos.
     */
    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntidade(id);
        if (historicoRepository.existsByChamadoId(id) || comentarioRepository.existsByChamadoId(id)) {
            throw new ConflitoException(
                    "O chamado possui histórico de status ou comentários e não pode ser excluído.");
        }
        chamadoRepository.delete(chamado);
    }

    private Chamado buscarEntidade(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado", id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Categoria buscarCategoriaAtiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
        if (!categoria.isAtivo()) {
            throw new RegraNegocioException("A categoria informada está inativa.");
        }
        return categoria;
    }

    private static void garantirNaoFechado(Chamado chamado) {
        if (chamado.isFechado()) {
            throw new RegraNegocioException("Chamado fechado não pode receber alterações.");
        }
    }
}
