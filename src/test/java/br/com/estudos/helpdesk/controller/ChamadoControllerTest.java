package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.request.ChamadoFiltro;
import br.com.estudos.helpdesk.dto.request.ChamadoRequest;
import br.com.estudos.helpdesk.dto.response.ChamadoResponse;
import br.com.estudos.helpdesk.dto.response.HistoricoStatusResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerTest {

    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService service;

    private static ChamadoResponse resposta(Long id, StatusChamado status) {
        return new ChamadoResponse(id, "Notebook não liga", "Não liga na tomada", Prioridade.ALTA, status,
                new ResumoResponse(1L, "Ana Souza"), null, null, new ResumoResponse(10L, "Hardware"),
                AGORA, AGORA, null);
    }

    @Test
    void deveAbrirChamadoERetornar201ComLocation() throws Exception {
        when(service.abrir(any())).thenReturn(resposta(5L, StatusChamado.ABERTO));

        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "Notebook não liga", "descricao": "Não liga na tomada",
                                 "prioridade": "ALTA", "solicitanteId": 1, "categoriaId": 10}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.dataAbertura").value("2026-10-31T12:00:00Z"));

        ArgumentCaptor<ChamadoRequest> request = ArgumentCaptor.forClass(ChamadoRequest.class);
        verify(service).abrir(request.capture());
        assertThat(request.getValue().titulo()).isEqualTo("Notebook não liga");
        assertThat(request.getValue().prioridade()).isEqualTo(Prioridade.ALTA);
    }

    @Test
    void deveRetornar400ComErrosDeCadaCampoInvalido() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "   ", "solicitanteId": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros[*].campo").value(containsInAnyOrder(
                        "titulo", "descricao", "prioridade", "solicitanteId", "categoriaId")));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar400ParaPrioridadeInexistente() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo": "T", "descricao": "D", "prioridade": "URGENTE",
                                 "solicitanteId": 1, "categoriaId": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"));
    }

    @Test
    void deveRetornar400ParaJsonMalformado() throws Exception {
        mockMvc.perform(post("/api/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deveRetornar404ComProblemDetailParaChamadoInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado", 99L));

        mockMvc.perform(get("/api/chamados/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value(containsString("99")))
                .andExpect(jsonPath("$.instance").value("/api/chamados/99"));
    }

    @Test
    void deveRetornar400ParaIdNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/chamados/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("'id'")));
    }

    @Test
    void deveAlterarStatus() throws Exception {
        when(service.alterarStatus(eq(5L), any())).thenReturn(resposta(5L, StatusChamado.EM_ANDAMENTO));

        mockMvc.perform(patch("/api/chamados/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"EM_ANDAMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
    }

    @Test
    void deveRetornar422EmTransicaoDeStatusInvalida() throws Exception {
        when(service.alterarStatus(eq(5L), any()))
                .thenThrow(new RegraNegocioException("Transição de status inválida: ABERTO -> RESOLVIDO."));

        mockMvc.perform(patch("/api/chamados/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVIDO\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.detail").value(containsString("ABERTO -> RESOLVIDO")));
    }

    @Test
    void deveRetornar400QuandoNovoStatusNaoForInformado() throws Exception {
        mockMvc.perform(patch("/api/chamados/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"));
    }

    @Test
    void deveRetornar400QuandoEquipeDaAtribuicaoNaoForInformada() throws Exception {
        mockMvc.perform(patch("/api/chamados/5/atribuicao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"responsavelId\": 3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("equipeId"));
    }

    @Test
    void deveExcluirChamadoRetornando204() throws Exception {
        mockMvc.perform(delete("/api/chamados/5"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).excluir(5L);
    }

    @Test
    void deveRetornar409AoExcluirChamadoComMovimentacao() throws Exception {
        doThrow(new ConflitoException("O chamado possui histórico de status ou comentários."))
                .when(service).excluir(5L);

        mockMvc.perform(delete("/api/chamados/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de dados"));
    }

    @Test
    void deveRepassarFiltrosPaginacaoEOrdenacao() throws Exception {
        when(service.listar(any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 1, 5, 0, 0, false, true));

        mockMvc.perform(get("/api/chamados")
                        .param("status", "ABERTO")
                        .param("prioridade", "ALTA")
                        .param("responsavelId", "3")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "prioridade,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));

        ArgumentCaptor<ChamadoFiltro> filtro = ArgumentCaptor.forClass(ChamadoFiltro.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(filtro.capture(), pageable.capture());
        assertThat(filtro.getValue())
                .isEqualTo(new ChamadoFiltro(StatusChamado.ABERTO, Prioridade.ALTA, null, 3L, null, null));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageable.getValue().getSort().getOrderFor("prioridade").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void deveUsarOrdenacaoPadraoPorDataDeAberturaDecrescente() throws Exception {
        when(service.listar(any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/chamados")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).listar(any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("dataAbertura").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void deveRetornar400ParaFiltroComValorInvalido() throws Exception {
        mockMvc.perform(get("/api/chamados").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("status"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("valor inválido"));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornarHistoricoDoChamado() throws Exception {
        when(service.listarHistorico(5L)).thenReturn(List.of(
                new HistoricoStatusResponse(1L, StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO, AGORA)));

        mockMvc.perform(get("/api/chamados/5/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusAnterior").value("ABERTO"))
                .andExpect(jsonPath("$[0].statusNovo").value("EM_ANDAMENTO"));
    }

    @Test
    void deveRetornar500SemExporDetalhesInternos() throws Exception {
        when(service.buscarPorId(5L)).thenThrow(new IllegalStateException("falha interna jdbc:mysql://db"));

        mockMvc.perform(get("/api/chamados/5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
