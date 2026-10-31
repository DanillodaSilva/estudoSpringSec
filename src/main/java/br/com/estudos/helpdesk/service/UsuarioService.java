package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.UsuarioRequest;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.dto.response.UsuarioResponse;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.mapper.UsuarioMapper;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.ComentarioRepository;
import br.com.estudos.helpdesk.repository.EquipeRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EquipeRepository equipeRepository;
    private final ChamadoRepository chamadoRepository;
    private final ComentarioRepository comentarioRepository;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          EquipeRepository equipeRepository,
                          ChamadoRepository chamadoRepository,
                          ComentarioRepository comentarioRepository,
                          UsuarioMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.equipeRepository = equipeRepository;
        this.chamadoRepository = chamadoRepository;
        this.comentarioRepository = comentarioRepository;
        this.mapper = mapper;
    }

    public PaginaResponse<UsuarioResponse> listar(Pageable pageable) {
        return PaginaResponse.de(usuarioRepository.findAll(pageable), mapper::paraResponse);
    }

    public UsuarioResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflitoException("Já existe um usuário com o e-mail informado.");
        }
        Equipe equipe = buscarEquipeParaVinculo(request.equipeId());
        return mapper.paraResponse(usuarioRepository.save(mapper.paraEntidade(request, equipe)));
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidade(id);
        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflitoException("Já existe um usuário com o e-mail informado.");
        }

        Long equipeAtualId = usuario.getEquipe() == null ? null : usuario.getEquipe().getId();
        Equipe equipe = usuario.getEquipe();
        if (!Objects.equals(equipeAtualId, request.equipeId())) {
            // O responsável precisa pertencer à equipe do chamado; trocar de equipe quebraria essa regra.
            if (chamadoRepository.existsByResponsavelIdAndStatusNot(id, StatusChamado.FECHADO)) {
                throw new RegraNegocioException(
                        "O usuário é responsável por chamados ativos; reatribua-os antes de trocar de equipe.");
            }
            equipe = buscarEquipeParaVinculo(request.equipeId());
        }

        mapper.atualizarEntidade(usuario, request, equipe);
        // flush para que a data de atualização (@PreUpdate) já apareça na resposta
        usuarioRepository.flush();
        return mapper.paraResponse(usuario);
    }

    /**
     * Usuários com chamados ou comentários não são excluídos fisicamente: devem ser desativados.
     */
    @Transactional
    public void excluir(Long id) {
        Usuario usuario = buscarEntidade(id);
        if (chamadoRepository.existsBySolicitanteIdOrResponsavelId(id, id)
                || comentarioRepository.existsByAutorId(id)) {
            throw new ConflitoException(
                    "O usuário possui chamados ou comentários vinculados e não pode ser excluído; desative-o.");
        }
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    private Equipe buscarEquipeParaVinculo(Long equipeId) {
        if (equipeId == null) {
            return null;
        }
        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", equipeId));
        if (!equipe.isAtivo()) {
            throw new RegraNegocioException("Não é possível vincular usuários a uma equipe inativa.");
        }
        return equipe;
    }
}
