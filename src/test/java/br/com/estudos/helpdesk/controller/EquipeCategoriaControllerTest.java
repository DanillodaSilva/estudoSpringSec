package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.response.CategoriaResponse;
import br.com.estudos.helpdesk.dto.response.EquipeResponse;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.exception.RecursoNaoEncontradoException;
import br.com.estudos.helpdesk.service.CategoriaService;
import br.com.estudos.helpdesk.service.EquipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({EquipeController.class, CategoriaController.class})
class EquipeCategoriaControllerTest {

    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquipeService equipeService;

    @MockitoBean
    private CategoriaService categoriaService;

    @Test
    void deveCriarEquipeERetornar201() throws Exception {
        when(equipeService.criar(any()))
                .thenReturn(new EquipeResponse(4L, "Redes", null, true, List.of(), AGORA, AGORA));

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Redes\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/equipes/4"))
                .andExpect(jsonPath("$.membros").isEmpty());
    }

    @Test
    void deveRetornar409AoExcluirEquipeComChamadosAtivos() throws Exception {
        doThrow(new ConflitoException("A equipe possui chamados ativos e não pode ser excluída."))
                .when(equipeService).excluir(1L);

        mockMvc.perform(delete("/api/equipes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A equipe possui chamados ativos e não pode ser excluída."));
    }

    @Test
    void deveRetornar404ParaEquipeInexistente() throws Exception {
        when(equipeService.buscarPorId(99L)).thenThrow(new RecursoNaoEncontradoException("Equipe", 99L));

        mockMvc.perform(get("/api/equipes/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaCategoriaSemNome() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"Sem nome\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("nome"));
    }

    @Test
    void deveCriarCategoriaERetornar201() throws Exception {
        when(categoriaService.criar(any()))
                .thenReturn(new CategoriaResponse(6L, "Telefonia", null, true, AGORA, AGORA));

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Telefonia\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/categorias/6"));
    }

    @Test
    void deveExcluirCategoriaRetornando204() throws Exception {
        mockMvc.perform(delete("/api/categorias/6"))
                .andExpect(status().isNoContent());

        verify(categoriaService).excluir(6L);
    }

    @Test
    void deveRetornar405ParaMetodoNaoSuportado() throws Exception {
        mockMvc.perform(post("/api/categorias/6"))
                .andExpect(status().isMethodNotAllowed());
    }
}
