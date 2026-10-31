package br.com.estudos.helpdesk;

import br.com.estudos.helpdesk.support.MySqlContainerConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo ponta a ponta pela API HTTP com a aplicação completa e MySQL real (Flyway + JPA + transações).
 * Os dados não são revertidos entre testes; por isso nomes e e-mails recebem um sufixo único.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MySqlContainerConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class ChamadoFluxoIT {

    @Autowired
    private MockMvc mockMvc;

    private final String sufixo = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void deveExecutarCicloCompletoDeAtendimento() throws Exception {
        long equipe = criar("/api/equipes", "{\"nome\": \"Suporte %s\"}".formatted(sufixo));
        long categoria = criar("/api/categorias", "{\"nome\": \"Hardware %s\"}".formatted(sufixo));
        long solicitante = criarUsuario("solicitante", null);
        long atendente = criarUsuario("atendente", equipe);
        long chamado = criar("/api/chamados", """
                {"titulo": "Notebook não liga", "descricao": "Não liga nem na tomada", "prioridade": "ALTA",
                 "solicitanteId": %d, "categoriaId": %d}""".formatted(solicitante, categoria));

        mockMvc.perform(get("/api/chamados/{id}", chamado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.titulo").value("Notebook não liga"));

        enviar(patch("/api/chamados/{id}/atribuicao", chamado),
                "{\"equipeId\": %d, \"responsavelId\": %d}".formatted(equipe, atendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsavel.id").value(atendente));

        for (String status : new String[]{"EM_ANDAMENTO", "RESOLVIDO", "FECHADO"}) {
            alterarStatus(chamado, status).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/chamados/{id}", chamado))
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.dataEncerramento").value(notNullValue()));

        mockMvc.perform(get("/api/chamados/{id}/historico", chamado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].statusAnterior").value("ABERTO"))
                .andExpect(jsonPath("$[2].statusNovo").value("FECHADO"));

        mockMvc.perform(get("/api/chamados").param("responsavelId", String.valueOf(atendente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));

        // Chamado fechado: sem novas operações e sem exclusões que apaguem o histórico
        enviar(post("/api/chamados/{id}/comentarios", chamado),
                "{\"autorId\": %d, \"conteudo\": \"Ainda com defeito\"}".formatted(solicitante))
                .andExpect(status().isUnprocessableContent());
        alterarStatus(chamado, "EM_ANDAMENTO").andExpect(status().isUnprocessableContent());
        mockMvc.perform(delete("/api/chamados/{id}", chamado)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/usuarios/{id}", atendente)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/equipes/{id}", equipe)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/categorias/{id}", categoria)).andExpect(status().isConflict());
    }

    @Test
    void regraVioladaNaoDeveDeixarAlteracaoParcial() throws Exception {
        long equipe = criar("/api/equipes", "{\"nome\": \"Rede %s\"}".formatted(sufixo));
        long outraEquipe = criar("/api/equipes", "{\"nome\": \"Sistemas %s\"}".formatted(sufixo));
        long categoria = criar("/api/categorias", "{\"nome\": \"Rede %s\"}".formatted(sufixo));
        long solicitante = criarUsuario("solicitante", null);
        long deOutraEquipe = criarUsuario("outro", outraEquipe);
        long chamado = criar("/api/chamados", """
                {"titulo": "Sem internet", "descricao": "Cabo ok, sem IP", "prioridade": "MEDIA",
                 "solicitanteId": %d, "categoriaId": %d}""".formatted(solicitante, categoria));

        enviar(patch("/api/chamados/{id}/atribuicao", chamado),
                "{\"equipeId\": %d, \"responsavelId\": %d}".formatted(equipe, deOutraEquipe))
                .andExpect(status().isUnprocessableContent());
        alterarStatus(chamado, "RESOLVIDO").andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/api/chamados/{id}", chamado))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.equipe").doesNotExist())
                .andExpect(jsonPath("$.responsavel").doesNotExist());
        mockMvc.perform(get("/api/chamados/{id}/historico", chamado))
                .andExpect(jsonPath("$.length()").value(0));

        // Sem movimentação, o chamado pode ser excluído fisicamente
        mockMvc.perform(delete("/api/chamados/{id}", chamado)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/chamados/{id}", chamado)).andExpect(status().isNotFound());
    }

    @Test
    void deveGerenciarComentariosEnquantoChamadoNaoEstiverFechado() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\": \"Software %s\"}".formatted(sufixo));
        long autor = criarUsuario("autor", null);
        long chamado = criar("/api/chamados", """
                {"titulo": "Instalar IDE", "descricao": "Preciso da IDE", "prioridade": "BAIXA",
                 "solicitanteId": %d, "categoriaId": %d}""".formatted(autor, categoria));
        long comentario = criar("/api/chamados/" + chamado + "/comentarios",
                "{\"autorId\": %d, \"conteudo\": \"Versão mais recente, por favor\"}".formatted(autor));

        enviar(put("/api/chamados/{c}/comentarios/{id}", chamado, comentario), "{\"conteudo\": \"Qualquer versão\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").value("Qualquer versão"));
        mockMvc.perform(get("/api/chamados/{id}/comentarios", chamado))
                .andExpect(jsonPath("$.totalElementos").value(1));

        // Com comentário, o chamado não pode ser excluído; após remover o comentário, pode.
        mockMvc.perform(delete("/api/chamados/{id}", chamado)).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/chamados/{c}/comentarios/{id}", chamado, comentario))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/chamados/{id}", chamado)).andExpect(status().isNoContent());
    }

    @Test
    void deveRejeitarEmailDuplicadoMesmoComMaiusculas() throws Exception {
        criarUsuario("unico", null);

        enviar(post("/api/usuarios"), "{\"nome\": \"Cópia\", \"email\": \"UNICO.%s@EXAMPLE.COM\"}".formatted(sufixo))
                .andExpect(status().isConflict());
    }

    @Test
    void deveExcluirUsuarioSemVinculosEDesativarUsuarioComVinculos() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\": \"Acesso %s\"}".formatted(sufixo));
        long semVinculos = criarUsuario("temporario", null);
        long comVinculos = criarUsuario("vinculado", null);
        criar("/api/chamados", """
                {"titulo": "Senha do ERP expirada", "descricao": "Preciso de reset", "prioridade": "ALTA",
                 "solicitanteId": %d, "categoriaId": %d}""".formatted(comVinculos, categoria));

        mockMvc.perform(delete("/api/usuarios/{id}", semVinculos)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/usuarios/{id}", comVinculos)).andExpect(status().isConflict());
        enviar(put("/api/usuarios/{id}", comVinculos),
                "{\"nome\": \"Vinculado\", \"email\": \"vinculado.%s@example.com\", \"ativo\": false}".formatted(sufixo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // Usuário inativo não abre novos chamados
        enviar(post("/api/chamados"), """
                {"titulo": "Outro", "descricao": "Outro", "prioridade": "BAIXA",
                 "solicitanteId": %d, "categoriaId": %d}""".formatted(comVinculos, categoria))
                .andExpect(status().isUnprocessableContent());
    }

    private long criarUsuario(String prefixo, Long equipeId) throws Exception {
        return criar("/api/usuarios", "{\"nome\": \"%s\", \"email\": \"%s.%s@example.com\", \"equipeId\": %s}"
                .formatted(prefixo, prefixo, sufixo, equipeId));
    }

    private long criar(String uri, String json) throws Exception {
        String corpo = enviar(post(uri), json)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private ResultActions alterarStatus(long chamado, String status) throws Exception {
        return enviar(patch("/api/chamados/{id}/status", chamado), "{\"status\": \"%s\"}".formatted(status));
    }

    private ResultActions enviar(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicao,
            String json) throws Exception {
        return mockMvc.perform(requisicao.contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
