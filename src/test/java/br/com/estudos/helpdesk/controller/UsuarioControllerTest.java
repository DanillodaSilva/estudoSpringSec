package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.dto.response.UsuarioResponse;
import br.com.estudos.helpdesk.exception.ConflitoException;
import br.com.estudos.helpdesk.service.UsuarioService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    private static final Instant AGORA = Instant.parse("2026-10-31T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService service;

    private static UsuarioResponse resposta(boolean ativo) {
        return new UsuarioResponse(8L, "Helena Prado", "helena@example.com", ativo, null, AGORA, AGORA);
    }

    @Test
    void deveCriarUsuarioERetornar201ComLocation() throws Exception {
        when(service.criar(any())).thenReturn(resposta(true));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Helena Prado\", \"email\": \"helena@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/usuarios/8"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.equipe").doesNotExist());
    }

    @Test
    void deveRetornar400ParaEmailInvalido() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Helena\", \"email\": \"nao-e-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("email"))
                .andExpect(jsonPath("$.erros[0].mensagem").value("O e-mail deve ser válido"));

        verifyNoInteractions(service);
    }

    @Test
    void deveRetornar409ParaEmailDuplicado() throws Exception {
        when(service.criar(any())).thenThrow(new ConflitoException("Já existe um usuário com o e-mail informado."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Helena\", \"email\": \"helena@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Já existe um usuário com o e-mail informado."));
    }

    @Test
    void deveDesativarUsuarioViaPut() throws Exception {
        when(service.atualizar(eq(8L), any())).thenReturn(resposta(false));

        mockMvc.perform(put("/api/usuarios/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Helena Prado\", \"email\": \"helena@example.com\", \"ativo\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornar409AoExcluirUsuarioComVinculos() throws Exception {
        doThrow(new ConflitoException("O usuário possui chamados ou comentários vinculados."))
                .when(service).excluir(8L);

        mockMvc.perform(delete("/api/usuarios/8"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRetornarListaVaziaComStatus200() throws Exception {
        when(service.listar(any())).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray())
                .andExpect(jsonPath("$.conteudo").isEmpty());
    }
}
