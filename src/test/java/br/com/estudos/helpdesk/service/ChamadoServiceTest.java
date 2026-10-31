package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.AlteracaoStatusRequest;
import br.com.estudos.helpdesk.dto.request.AtribuicaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoFiltro;
import br.com.estudos.helpdesk.dto.request.ChamadoRequest;
import br.com.estudos.helpdesk.dto.response.ChamadoResponse;
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
import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.CategoriaRepository;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.ComentarioRepository;
import br.com.estudos.helpdesk.repository.EquipeRepository;
import br.com.estudos.helpdesk.repository.HistoricoStatusRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static br.com.estudos.helpdesk.model.enums.StatusChamado.ABERTO;
import static br.com.estudos.helpdesk.model.enums.StatusChamado.AGUARDANDO;
import static br.com.estudos.helpdesk.model.enums.StatusChamado.EM_ANDAMENTO;
import static br.com.estudos.helpdesk.model.enums.StatusChamado.FECHADO;
import static br.com.estudos.helpdesk.model.enums.StatusChamado.RESOLVIDO;
import static br.com.estudos.helpdesk.support.Fixtures.categoria;
import static br.com.estudos.helpdesk.support.Fixtures.chamado;
import static br.com.estudos.helpdesk.support.Fixtures.equipe;
import static br.com.estudos.helpdesk.support.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipeRepository equipeRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private HistoricoStatusRepository historicoRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Spy
    private ChamadoMapper mapper = new ChamadoMapper();

    @InjectMocks
    private ChamadoService service;

    private final Categoria categoria = categoria(10L, true);
    private final Usuario solicitante = usuario(1L, true, null);

    private Chamado chamadoExistente(StatusChamado status) {
        Chamado chamado = chamado(5L, status, solicitante, categoria);
        when(chamadoRepository.findById(5L)).thenReturn(Optional.of(chamado));
        return chamado;
    }

    @Nested
    class Abertura {

        private final ChamadoRequest request =
                new ChamadoRequest("Impressora", "Não imprime", Prioridade.MEDIA, 1L, 10L);

        @Test
        void deveAbrirChamadoComStatusAbertoESemResponsavel() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
            when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria));
            when(chamadoRepository.save(any(Chamado.class))).thenAnswer(inv -> inv.getArgument(0));

            ChamadoResponse resposta = service.abrir(request);

            assertThat(resposta.status()).isEqualTo(ABERTO);
            assertThat(resposta.solicitante().id()).isEqualTo(1L);
            assertThat(resposta.categoria().id()).isEqualTo(10L);
            assertThat(resposta.responsavel()).isNull();
            assertThat(resposta.equipe()).isNull();
            assertThat(resposta.dataEncerramento()).isNull();
            verifyNoInteractions(historicoRepository);
        }

        @Test
        void naoDeveAbrirChamadoParaSolicitanteInativo() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, false, null)));

            assertThatThrownBy(() -> service.abrir(request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("solicitante");
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void naoDeveAbrirChamadoComCategoriaInativa() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(solicitante));
            when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, false)));

            assertThatThrownBy(() -> service.abrir(request))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("categoria");
            verify(chamadoRepository, never()).save(any());
        }

        @Test
        void deveFalharQuandoSolicitanteNaoExiste() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.abrir(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    class AlteracaoDeStatus {

        @Test
        void deveAplicarTransicaoValidaERegistrarHistorico() {
            Chamado chamado = chamadoExistente(ABERTO);

            ChamadoResponse resposta = service.alterarStatus(5L, new AlteracaoStatusRequest(EM_ANDAMENTO));

            ArgumentCaptor<HistoricoStatus> historico = ArgumentCaptor.forClass(HistoricoStatus.class);
            verify(historicoRepository).save(historico.capture());
            assertThat(historico.getValue().getChamado()).isSameAs(chamado);
            assertThat(historico.getValue().getStatusAnterior()).isEqualTo(ABERTO);
            assertThat(historico.getValue().getStatusNovo()).isEqualTo(EM_ANDAMENTO);
            assertThat(resposta.status()).isEqualTo(EM_ANDAMENTO);
            assertThat(resposta.dataEncerramento()).isNull();
        }

        @Test
        void deveRejeitarTransicaoInvalidaSemGerarHistorico() {
            Chamado chamado = chamadoExistente(ABERTO);

            assertThatThrownBy(() -> service.alterarStatus(5L, new AlteracaoStatusRequest(RESOLVIDO)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ABERTO -> RESOLVIDO");
            assertThat(chamado.getStatus()).isEqualTo(ABERTO);
            verify(historicoRepository, never()).save(any());
        }

        @Test
        void deveRejeitarAlteracaoParaOMesmoStatus() {
            chamadoExistente(AGUARDANDO);

            assertThatThrownBy(() -> service.alterarStatus(5L, new AlteracaoStatusRequest(AGUARDANDO)))
                    .isInstanceOf(RegraNegocioException.class);
            verify(historicoRepository, never()).save(any());
        }

        @Test
        void deveRegistrarDataDeEncerramentoAoFechar() {
            Chamado chamado = chamadoExistente(RESOLVIDO);

            ChamadoResponse resposta = service.alterarStatus(5L, new AlteracaoStatusRequest(FECHADO));

            assertThat(chamado.getDataEncerramento()).isNotNull();
            assertThat(resposta.status()).isEqualTo(FECHADO);
            assertThat(resposta.dataEncerramento()).isEqualTo(chamado.getDataEncerramento());
            verify(historicoRepository).save(any(HistoricoStatus.class));
        }

        @Test
        void devePermitirReaberturaDeChamadoResolvido() {
            chamadoExistente(RESOLVIDO);

            ChamadoResponse resposta = service.alterarStatus(5L, new AlteracaoStatusRequest(EM_ANDAMENTO));

            assertThat(resposta.status()).isEqualTo(EM_ANDAMENTO);
            assertThat(resposta.dataEncerramento()).isNull();
        }

        @Test
        void naoDeveAlterarStatusDeChamadoFechado() {
            chamadoExistente(FECHADO);

            assertThatThrownBy(() -> service.alterarStatus(5L, new AlteracaoStatusRequest(EM_ANDAMENTO)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("fechado");
            verify(historicoRepository, never()).save(any());
        }
    }

    @Nested
    class Atribuicao {

        private final Equipe suporte = equipe(20L, true);

        @Test
        void deveAtribuirEquipeEResponsavelAtivoQuePertenceAEla() {
            Chamado chamado = chamadoExistente(ABERTO);
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, true, suporte)));

            ChamadoResponse resposta = service.atribuir(5L, new AtribuicaoRequest(20L, 3L));

            assertThat(resposta.equipe().id()).isEqualTo(20L);
            assertThat(resposta.responsavel().id()).isEqualTo(3L);
            assertThat(chamado.getResponsavel().getId()).isEqualTo(3L);
        }

        @Test
        void deveAtribuirSomenteEquipeRemovendoResponsavelAnterior() {
            Chamado chamado = chamadoExistente(EM_ANDAMENTO);
            chamado.setEquipe(suporte);
            chamado.setResponsavel(usuario(3L, true, suporte));
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));

            ChamadoResponse resposta = service.atribuir(5L, new AtribuicaoRequest(20L, null));

            assertThat(resposta.equipe().id()).isEqualTo(20L);
            assertThat(resposta.responsavel()).isNull();
        }

        @Test
        void naoDeveAtribuirUsuarioInativo() {
            Chamado chamado = chamadoExistente(ABERTO);
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, false, suporte)));

            assertThatThrownBy(() -> service.atribuir(5L, new AtribuicaoRequest(20L, 3L)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("inativo");
            assertThat(chamado.getResponsavel()).isNull();
            assertThat(chamado.getEquipe()).isNull();
        }

        @Test
        void naoDeveAtribuirResponsavelDeOutraEquipe() {
            chamadoExistente(ABERTO);
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, true, equipe(99L, true))));

            assertThatThrownBy(() -> service.atribuir(5L, new AtribuicaoRequest(20L, 3L)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("pertencer à equipe");
        }

        @Test
        void naoDeveAtribuirResponsavelSemEquipe() {
            chamadoExistente(ABERTO);
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, true, null)));

            assertThatThrownBy(() -> service.atribuir(5L, new AtribuicaoRequest(20L, 3L)))
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void naoDeveAtribuirAEquipeInativa() {
            chamadoExistente(ABERTO);
            when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe(20L, false)));

            assertThatThrownBy(() -> service.atribuir(5L, new AtribuicaoRequest(20L, null)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("equipe inativa");
        }

        @Test
        void naoDeveAtribuirChamadoFechado() {
            chamadoExistente(FECHADO);

            assertThatThrownBy(() -> service.atribuir(5L, new AtribuicaoRequest(20L, 3L)))
                    .isInstanceOf(RegraNegocioException.class);
            verifyNoInteractions(equipeRepository);
        }
    }

    @Nested
    class Atualizacao {

        private final ChamadoAtualizacaoRequest request =
                new ChamadoAtualizacaoRequest("Novo título", "Nova descrição", Prioridade.CRITICA, 10L);

        @Test
        void deveAtualizarDadosDescritivos() {
            chamadoExistente(EM_ANDAMENTO);

            ChamadoResponse resposta = service.atualizar(5L, request);

            assertThat(resposta.titulo()).isEqualTo("Novo título");
            assertThat(resposta.prioridade()).isEqualTo(Prioridade.CRITICA);
            assertThat(resposta.status()).isEqualTo(EM_ANDAMENTO);
            verifyNoInteractions(categoriaRepository);
        }

        @Test
        void naoDeveTrocarParaCategoriaInativa() {
            chamadoExistente(ABERTO);
            when(categoriaRepository.findById(11L)).thenReturn(Optional.of(categoria(11L, false)));

            assertThatThrownBy(() -> service.atualizar(5L,
                    new ChamadoAtualizacaoRequest("Título", "Descrição", Prioridade.BAIXA, 11L)))
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void naoDeveAtualizarChamadoFechado() {
            Chamado chamado = chamadoExistente(FECHADO);

            assertThatThrownBy(() -> service.atualizar(5L, request))
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(chamado.getTitulo()).isEqualTo("Chamado 5");
        }
    }

    @Nested
    class Exclusao {

        @Test
        void deveExcluirChamadoSemMovimentacao() {
            Chamado chamado = chamadoExistente(ABERTO);
            when(historicoRepository.existsByChamadoId(5L)).thenReturn(false);
            when(comentarioRepository.existsByChamadoId(5L)).thenReturn(false);

            service.excluir(5L);

            verify(chamadoRepository).delete(chamado);
        }

        @Test
        void naoDeveExcluirChamadoComHistorico() {
            chamadoExistente(EM_ANDAMENTO);
            when(historicoRepository.existsByChamadoId(5L)).thenReturn(true);

            assertThatThrownBy(() -> service.excluir(5L)).isInstanceOf(ConflitoException.class);
            verify(chamadoRepository, never()).delete(any());
        }

        @Test
        void naoDeveExcluirChamadoComComentarios() {
            chamadoExistente(ABERTO);
            when(historicoRepository.existsByChamadoId(5L)).thenReturn(false);
            when(comentarioRepository.existsByChamadoId(5L)).thenReturn(true);

            assertThatThrownBy(() -> service.excluir(5L)).isInstanceOf(ConflitoException.class);
            verify(chamadoRepository, never()).delete(any());
        }

        @Test
        void deveFalharAoExcluirChamadoInexistente() {
            when(chamadoRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluir(404L)).isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Nested
    class Consulta {

        @Test
        void deveFalharAoBuscarChamadoInexistente() {
            when(chamadoRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(404L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("404");
        }

        @Test
        void deveFalharAoListarHistoricoDeChamadoInexistente() {
            when(chamadoRepository.existsById(404L)).thenReturn(false);

            assertThatThrownBy(() -> service.listarHistorico(404L))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verifyNoInteractions(historicoRepository);
        }

        @Test
        void deveRepassarFiltrosParaAConsultaERetornarPaginaVazia() {
            var pageable = PageRequest.of(0, 20, Sort.by("dataAbertura"));
            when(chamadoRepository.filtrar(ABERTO, Prioridade.ALTA, 10L, null, null, null, pageable))
                    .thenReturn(Page.empty(pageable));

            var pagina = service.listar(new ChamadoFiltro(ABERTO, Prioridade.ALTA, 10L, null, null, null), pageable);

            assertThat(pagina.conteudo()).isEmpty();
            assertThat(pagina.totalElementos()).isZero();
        }

        @Test
        void deveRejeitarOrdenacaoPorPropriedadeNaoPermitida() {
            var pageable = PageRequest.of(0, 20, Sort.by("solicitante.email"));
            var semFiltros = new ChamadoFiltro(null, null, null, null, null, null);

            assertThatThrownBy(() -> service.listar(semFiltros, pageable))
                    .isInstanceOf(ParametroInvalidoException.class);
            verifyNoInteractions(chamadoRepository);
        }

        @Test
        void deveListarHistoricoEmOrdem() {
            Chamado chamado = chamado(5L, AGUARDANDO, solicitante, categoria);
            when(chamadoRepository.existsById(5L)).thenReturn(true);
            when(historicoRepository.findByChamadoIdOrderByDataAlteracaoAscIdAsc(5L)).thenReturn(List.of(
                    new HistoricoStatus(chamado, ABERTO, EM_ANDAMENTO),
                    new HistoricoStatus(chamado, EM_ANDAMENTO, AGUARDANDO)));

            var historico = service.listarHistorico(5L);

            assertThat(historico).extracting("statusNovo").containsExactly(EM_ANDAMENTO, AGUARDANDO);
        }
    }
}
