package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.response.ComentarioResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.dto.response.ResumoResponse;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.exception.RegraNegocioException;
import br.com.estudos.helpdesk.service.ComentarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComentarioController.class)
class ComentarioControllerTest {

    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComentarioService service;

    private static ComentarioResponse resposta(String conteudo) {
        return new ComentarioResponse(7L, 5L, new ResumoResponse(3L, "Carla Mendes"), conteudo, AGORA, AGORA);
    }

    @Test
    void deveCriarComentarioERetornar201ComLocation() throws Exception {
        when(service.criar(eq(5L), any())).thenReturn(resposta("Verificando"));

        mockMvc.perform(post("/api/chamados/5/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\": 3, \"conteudo\": \"Verificando\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/chamados/5/comentarios/7"))
                .andExpect(jsonPath("$.chamadoId").value(5))
                .andExpect(jsonPath("$.autor.nome").value("Carla Mendes"));
    }

    @Test
    void deveRejeitarComentarioVazio() throws Exception {
        mockMvc.perform(post("/api/chamados/5/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\": 3, \"conteudo\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("conteudo"));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar422AoComentarEmChamadoFechado() throws Exception {
        when(service.criar(eq(5L), any()))
                .thenThrow(new RegraNegocioException("Não é possível comentar em um chamado fechado."));

        mockMvc.perform(post("/api/chamados/5/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autorId\": 3, \"conteudo\": \"Texto\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void deveListarComentariosPaginados() throws Exception {
        when(service.listar(eq(5L), any()))
                .thenReturn(new PaginaResponse<>(List.of(resposta("Primeiro")), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/chamados/5/comentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].conteudo").value("Primeiro"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void deveEditarComentario() throws Exception {
        when(service.atualizar(eq(5L), eq(7L), any())).thenReturn(resposta("Revisado"));

        mockMvc.perform(put("/api/chamados/5/comentarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"Revisado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value("Revisado"));
    }

    @Test
    void deveExcluirComentarioRetornando204() throws Exception {
        mockMvc.perform(delete("/api/chamados/5/comentarios/7"))
                .andExpect(status().isNoContent());

        verify(service).excluir(5L, 7L);
    }

    @Test
    void deveRetornar404ParaComentarioDeOutroChamado() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Comentário", 7L)).when(service).excluir(9L, 7L);

        mockMvc.perform(delete("/api/chamados/9/comentarios/7"))
                .andExpect(status().isNotFound());
    }
}
