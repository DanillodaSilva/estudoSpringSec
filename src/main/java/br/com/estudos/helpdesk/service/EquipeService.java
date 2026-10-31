package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.EquipeRequest;
import br.com.estudos.helpdesk.dto.response.EquipeResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.mapper.EquipeMapper;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.EquipeRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EquipeService {

    private final EquipeRepository equipeRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;
    private final EquipeMapper mapper;

    public EquipeService(EquipeRepository equipeRepository,
                         UsuarioRepository usuarioRepository,
                         ChamadoRepository chamadoRepository,
                         EquipeMapper mapper) {
        this.equipeRepository = equipeRepository;
        this.usuarioRepository = usuarioRepository;
        this.chamadoRepository = chamadoRepository;
        this.mapper = mapper;
    }

    public PaginaResponse<EquipeResponse> listar(Pageable pageable) {
        return PaginaResponse.de(equipeRepository.findAll(pageable), mapper::paraResponse);
    }

    public EquipeResponse buscarPorId(Long id) {
        return mapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public EquipeResponse criar(EquipeRequest request) {
        if (equipeRepository.existsByNome(request.nome())) {
            throw new ConflitoException("Já existe uma equipe com o nome '%s'.".formatted(request.nome()));
        }
        return mapper.paraResponse(equipeRepository.save(mapper.paraEntidade(request)));
    }

    @Transactional
    public EquipeResponse atualizar(Long id, EquipeRequest request) {
        Equipe equipe = buscarEntidade(id);
        if (equipeRepository.existsByNomeAndIdNot(request.nome(), id)) {
            throw new ConflitoException("Já existe uma equipe com o nome '%s'.".formatted(request.nome()));
        }
        mapper.atualizarEntidade(equipe, request);
        // flush para que a data de atualização (@PreUpdate) já apareça na resposta
        equipeRepository.flush();
        return mapper.paraResponse(equipe);
    }

    /**
     * Exclusão física só é permitida para equipes sem chamados (ativos ou encerrados) e sem membros.
     * Nos demais casos a equipe deve ser desativada, preservando o histórico.
     */
    @Transactional
    public void excluir(Long id) {
        Equipe equipe = buscarEntidade(id);
        if (chamadoRepository.existsByEquipeIdAndStatusNot(id, StatusChamado.FECHADO)) {
            throw new ConflitoException("A equipe possui chamados ativos e não pode ser excluída.");
        }
        if (chamadoRepository.existsByEquipeId(id)) {
            throw new ConflitoException(
                    "A equipe possui chamados encerrados em seu histórico e não pode ser excluída; desative-a.");
        }
        if (usuarioRepository.existsByEquipeId(id)) {
            throw new ConflitoException(
                    "A equipe possui membros; desvincule-os antes de excluí-la ou desative a equipe.");
        }
        equipeRepository.delete(equipe);
    }

    private Equipe buscarEntidade(Long id) {
        return equipeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", id));
    }
}
