package br.com.estudos.helpdesk.service;

import br.com.estudos.helpdesk.dto.request.CategoriaRequest;
import br.com.estudos.helpdesk.dto.response.CategoriaResponse;
import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.mapper.CategoriaMapper;
import br.com.estudos.helpdesk.repository.CategoriaRepository;
import br.com.estudos.helpdesk.repository.ChamadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.estudos.helpdesk.support.Fixtures.categoria;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ChamadoRepository chamadoRepository;
    @Spy
    private CategoriaMapper mapper = new CategoriaMapper();

    @InjectMocks
    private CategoriaService service;

    @Test
    void deveCriarCategoriaAtivaPorPadrao() {
        when(categoriaRepository.existsByNome("Rede")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaResponse resposta = service.criar(new CategoriaRequest(" Rede ", "Wi-Fi e VPN", null));

        assertThat(resposta.nome()).isEqualTo("Rede");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void naoDeveRenomearParaNomeJaUtilizado() {
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, true)));
        when(categoriaRepository.existsByNomeAndIdNot("Hardware", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(10L, new CategoriaRequest("Hardware", null, null)))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void naoDeveExcluirCategoriaUsadaPorChamados() {
        when(categoriaRepository.findById(10L)).thenReturn(Optional.of(categoria(10L, true)));
        when(chamadoRepository.existsByCategoriaId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(10L)).isInstanceOf(ConflitoException.class);
        verify(categoriaRepository, never()).delete(any());
    }

    @Test
    void deveFalharAoExcluirCategoriaInexistente() {
        when(categoriaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(404L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
