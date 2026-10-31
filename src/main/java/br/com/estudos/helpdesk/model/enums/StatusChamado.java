package br.com.estudos.helpdesk.model.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida do chamado. Transições permitidas:
 *
 * <pre>
 * ABERTO       -> EM_ANDAMENTO, AGUARDANDO, FECHADO
 * EM_ANDAMENTO -> AGUARDANDO, RESOLVIDO
 * AGUARDANDO   -> EM_ANDAMENTO, RESOLVIDO
 * RESOLVIDO    -> EM_ANDAMENTO (reabertura), FECHADO
 * FECHADO      -> (estado final)
 * </pre>
 */
public enum StatusChamado {
    ABERTO,
    EM_ANDAMENTO,
    AGUARDANDO,
    RESOLVIDO,
    FECHADO;

    private static final Map<StatusChamado, Set<StatusChamado>> TRANSICOES = new EnumMap<>(StatusChamado.class);

    static {
        TRANSICOES.put(ABERTO, EnumSet.of(EM_ANDAMENTO, AGUARDANDO, FECHADO));
        TRANSICOES.put(EM_ANDAMENTO, EnumSet.of(AGUARDANDO, RESOLVIDO));
        TRANSICOES.put(AGUARDANDO, EnumSet.of(EM_ANDAMENTO, RESOLVIDO));
        TRANSICOES.put(RESOLVIDO, EnumSet.of(EM_ANDAMENTO, FECHADO));
        TRANSICOES.put(FECHADO, EnumSet.noneOf(StatusChamado.class));
    }

    public boolean podeTransicionarPara(StatusChamado destino) {
        return TRANSICOES.get(this).contains(destino);
    }

    public Set<StatusChamado> transicoesPermitidas() {
        return Collections.unmodifiableSet(TRANSICOES.get(this));
    }
}
