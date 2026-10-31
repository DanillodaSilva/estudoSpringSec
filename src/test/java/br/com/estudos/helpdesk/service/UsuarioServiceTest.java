package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.UsuarioRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.estudos.helpdesk.support.Fixtures.equipe;
import static br.com.estudos.helpdesk.support.Fixtures.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private EquipeRepository equipeRepository;
    @Mock
    private ChamadoRepository chamadoRepository;
    @Mock
    private ComentarioRepository comentarioRepository;
    @Spy
    private UsuarioMapper mapper = new UsuarioMapper();

    @InjectMocks
    private UsuarioService service;

    @Test
    void deveNormalizarEmailECriarUsuarioAtivoPorPadrao() {
        Equipe suporte = equipe(20L, true);
        var request = new UsuarioRequest("  Maria  ", "  Maria@Example.COM ", 20L, null);
        when(usuarioRepository.existsByEmail("maria@example.com")).thenReturn(false);
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(suporte));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = service.criar(request);

        assertThat(resposta.nome()).isEqualTo("Maria");
        assertThat(resposta.email()).isEqualTo("maria@example.com");
        assertThat(resposta.ativo()).isTrue();
        assertThat(resposta.equipe().id()).isEqualTo(20L);
    }

    @Test
    void naoDeveCriarUsuarioComEmailDuplicado() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@example.com", null, null)))
                .isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoDeveVincularUsuarioAEquipeInativa() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe(20L, false)));

        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@example.com", 20L, null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void naoDeveAtualizarParaEmailDeOutroUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, true, null)));
        when(usuarioRepository.existsByEmailAndIdNot("outro@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, new UsuarioRequest("Ana", "outro@example.com", null, true)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void deveDesativarUsuario() {
        Usuario usuario = usuario(1L, true, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioResponse resposta = service.atualizar(1L,
                new UsuarioRequest("Usuário 1", "usuario1@example.com", null, false));

        assertThat(resposta.ativo()).isFalse();
        assertThat(usuario.isAtivo()).isFalse();
    }

    @Test
    void naoDeveTrocarDeEquipeUsuarioResponsavelPorChamadosAtivos() {
        Usuario usuario = usuario(3L, true, equipe(20L, true));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));
        when(chamadoRepository.existsByResponsavelIdAndStatusNot(3L, StatusChamado.FECHADO)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(3L,
                new UsuarioRequest("Usuário 3", "usuario3@example.com", 21L, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("reatribua");
        assertThat(usuario.getEquipe().getId()).isEqualTo(20L);
    }

    @Test
    void deveExcluirUsuarioSemVinculos() {
        Usuario usuario = usuario(1L, true, null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(chamadoRepository.existsBySolicitanteIdOrResponsavelId(1L, 1L)).thenReturn(false);
        when(comentarioRepository.existsByAutorId(1L)).thenReturn(false);

        service.excluir(1L);

        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void naoDeveExcluirUsuarioVinculadoAChamados() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, true, null)));
        when(chamadoRepository.existsBySolicitanteIdOrResponsavelId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void naoDeveExcluirUsuarioAutorDeComentarios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, true, null)));
        when(chamadoRepository.existsBySolicitanteIdOrResponsavelId(1L, 1L)).thenReturn(false);
        when(comentarioRepository.existsByAutorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(1L)).isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void deveFalharAoBuscarUsuarioInexistente() {
        when(usuarioRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(404L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
