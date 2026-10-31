package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.ComentarioAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ComentarioRequest;
import br.com.estudos.helpdesk.dto.response.ComentarioResponse;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Comentario;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.mapper.ComentarioMapper;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import br.com.estudos.helpdesk.repository.ComentarioRepository;
import br.com.estudos.helpdesk.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static br.com.estudos.helpdesk.support.Fixtures.categoria;
import static br.com.estudos.helpdesk.support.Fixtures.chamado;
import static br.com.estudos.helpdesk.support.Fixtures.comentario;
import static br.com.estudos.helpdesk.support.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;
    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Spy
    private ComentarioMapper mapper = new ComentarioMapper();

    @InjectMocks
    private ComentarioService service;

    private final Usuario autor = usuario(3L, true, null);

    private Chamado chamadoCom(StatusChamado status) {
        return chamado(5L, status, usuario(1L, true, null), categoria(10L, true));
    }

    @Test
    void deveCriarComentarioEmChamadoAberto() {
        Chamado chamado = chamadoCom(StatusChamado.EM_ANDAMENTO);
        when(chamadoRepository.findById(5L)).thenReturn(Optional.of(chamado));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(autor));
        when(comentarioRepository.save(any(Comentario.class))).thenAnswer(inv -> inv.getArgument(0));

        ComentarioResponse resposta = service.criar(5L, new ComentarioRequest(3L, "Verificando o equipamento"));

        assertThat(resposta.chamadoId()).isEqualTo(5L);
        assertThat(resposta.autor().id()).isEqualTo(3L);
        assertThat(resposta.conteudo()).isEqualTo("Verificando o equipamento");
    }

    @Test
    void naoDeveComentarEmChamadoFechado() {
        when(chamadoRepository.findById(5L)).thenReturn(Optional.of(chamadoCom(StatusChamado.FECHADO)));

        assertThatThrownBy(() -> service.criar(5L, new ComentarioRequest(3L, "Texto")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fechado");
        verifyNoInteractions(usuarioRepository);
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void naoDeveAceitarAutorInativo() {
        when(chamadoRepository.findById(5L)).thenReturn(Optional.of(chamadoCom(StatusChamado.ABERTO)));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, false, null)));

        assertThatThrownBy(() -> service.criar(5L, new ComentarioRequest(3L, "Texto")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void naoDeveAceitarAutorInexistente() {
        when(chamadoRepository.findById(5L)).thenReturn(Optional.of(chamadoCom(StatusChamado.ABERTO)));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(5L, new ComentarioRequest(3L, "Texto")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveComentarEmChamadoInexistente() {
        when(chamadoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(5L, new ComentarioRequest(3L, "Texto")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Chamado");
    }

    @Test
    void deveEditarComentarioDeChamadoNaoFechado() {
        Comentario comentario = comentario(7L, autor, chamadoCom(StatusChamado.AGUARDANDO));
        when(comentarioRepository.findByIdAndChamadoId(7L, 5L)).thenReturn(Optional.of(comentario));

        ComentarioResponse resposta = service.atualizar(5L, 7L, new ComentarioAtualizacaoRequest("Texto revisado"));

        assertThat(resposta.conteudo()).isEqualTo("Texto revisado");
        assertThat(resposta.autor().id()).isEqualTo(3L);
    }

    @Test
    void naoDeveEditarComentarioDeChamadoFechado() {
        Comentario comentario = comentario(7L, autor, chamadoCom(StatusChamado.FECHADO));
        when(comentarioRepository.findByIdAndChamadoId(7L, 5L)).thenReturn(Optional.of(comentario));

        assertThatThrownBy(() -> service.atualizar(5L, 7L, new ComentarioAtualizacaoRequest("Novo texto")))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(comentario.getConteudo()).isEqualTo("Comentário 7");
    }

    @Test
    void deveExcluirComentarioDeChamadoNaoFechado() {
        Comentario comentario = comentario(7L, autor, chamadoCom(StatusChamado.ABERTO));
        when(comentarioRepository.findByIdAndChamadoId(7L, 5L)).thenReturn(Optional.of(comentario));

        service.excluir(5L, 7L);

        verify(comentarioRepository).delete(comentario);
    }

    @Test
    void naoDeveExcluirComentarioDeChamadoFechado() {
        Comentario comentario = comentario(7L, autor, chamadoCom(StatusChamado.FECHADO));
        when(comentarioRepository.findByIdAndChamadoId(7L, 5L)).thenReturn(Optional.of(comentario));

        assertThatThrownBy(() -> service.excluir(5L, 7L)).isInstanceOf(RegraNegocioException.class);
        verify(comentarioRepository, never()).delete(any());
    }

    @Test
    void deveTratarComentarioDeOutroChamadoComoInexistente() {
        when(comentarioRepository.findByIdAndChamadoId(7L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L, 7L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveFalharAoListarComentariosDeChamadoInexistente() {
        when(chamadoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listar(99L, PageRequest.of(0, 20)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(comentarioRepository);
    }
}
