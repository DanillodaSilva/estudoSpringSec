package br.com.estudos.helpdesk.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class StatusChamadoTest {

    @ParameterizedTest(name = "{0} -> {1} é permitida")
    @CsvSource({
            "ABERTO, EM_ANDAMENTO",
            "ABERTO, AGUARDANDO",
            "ABERTO, FECHADO",
            "EM_ANDAMENTO, AGUARDANDO",
            "EM_ANDAMENTO, RESOLVIDO",
            "AGUARDANDO, EM_ANDAMENTO",
            "AGUARDANDO, RESOLVIDO",
            "RESOLVIDO, EM_ANDAMENTO",
            "RESOLVIDO, FECHADO"
    })
    void devePermitirTransicoesDocumentadas(StatusChamado origem, StatusChamado destino) {
        assertThat(origem.podeTransicionarPara(destino)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} é proibida")
    @CsvSource({
            "ABERTO, RESOLVIDO",
            "EM_ANDAMENTO, ABERTO",
            "EM_ANDAMENTO, FECHADO",
            "AGUARDANDO, FECHADO",
            "RESOLVIDO, ABERTO",
            "RESOLVIDO, AGUARDANDO"
    })
    void deveRejeitarTransicoesNaoDocumentadas(StatusChamado origem, StatusChamado destino) {
        assertThat(origem.podeTransicionarPara(destino)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(StatusChamado.class)
    void naoDevePermitirTransicaoParaOMesmoStatus(StatusChamado status) {
        assertThat(status.podeTransicionarPara(status)).isFalse();
    }

    @Test
    void fechadoDeveSerEstadoFinal() {
        assertThat(StatusChamado.FECHADO.transicoesPermitidas()).isEmpty();
    }
}
