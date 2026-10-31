package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.EquipeRequest;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.mapper.EquipeMapper;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipeServiceTest {

    @Mock
    private EquipeRepository equipeRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private ChamadoRepository chamadoRepository;
    @Spy
    private EquipeMapper mapper = new EquipeMapper();

    @InjectMocks
    private EquipeService service;

    @Test
    void naoDeveCriarEquipeComNomeDuplicado() {
        when(equipeRepository.existsByNome("Suporte")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new EquipeRequest("Suporte", null, null)))
                .isInstanceOf(ConflitoException.class);
        verify(equipeRepository, never()).save(any());
    }

    @Test
    void naoDeveExcluirEquipeComChamadosAtivos() {
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe(20L, true)));
        when(chamadoRepository.existsByEquipeIdAndStatusNot(20L, StatusChamado.FECHADO)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(20L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("chamados ativos");
        verify(equipeRepository, never()).delete(any());
    }

    @Test
    void naoDeveExcluirEquipeComChamadosEncerradosNoHistorico() {
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe(20L, true)));
        when(chamadoRepository.existsByEquipeIdAndStatusNot(20L, StatusChamado.FECHADO)).thenReturn(false);
        when(chamadoRepository.existsByEquipeId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(20L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("desative");
    }

    @Test
    void naoDeveExcluirEquipeComMembros() {
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe(20L, true)));
        when(chamadoRepository.existsByEquipeIdAndStatusNot(20L, StatusChamado.FECHADO)).thenReturn(false);
        when(chamadoRepository.existsByEquipeId(20L)).thenReturn(false);
        when(usuarioRepository.existsByEquipeId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(20L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("membros");
    }

    @Test
    void deveExcluirEquipeSemVinculos() {
        Equipe equipe = equipe(20L, true);
        when(equipeRepository.findById(20L)).thenReturn(Optional.of(equipe));
        when(chamadoRepository.existsByEquipeIdAndStatusNot(20L, StatusChamado.FECHADO)).thenReturn(false);
        when(chamadoRepository.existsByEquipeId(20L)).thenReturn(false);
        when(usuarioRepository.existsByEquipeId(20L)).thenReturn(false);

        service.excluir(20L);

        verify(equipeRepository).delete(equipe);
    }
}
