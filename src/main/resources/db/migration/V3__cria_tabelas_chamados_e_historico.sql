CREATE TABLE chamados (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    titulo            VARCHAR(150)  NOT NULL,
    descricao         VARCHAR(4000) NOT NULL,
    prioridade        VARCHAR(20)   NOT NULL,
    status            VARCHAR(20)   NOT NULL,
    solicitante_id    BIGINT        NOT NULL,
    responsavel_id    BIGINT        NULL,
    equipe_id         BIGINT        NULL,
    categoria_id      BIGINT        NOT NULL,
    data_abertura     DATETIME(6)   NOT NULL,
    data_atualizacao  DATETIME(6)   NOT NULL,
    data_encerramento DATETIME(6)   NULL,
    CONSTRAINT pk_chamados PRIMARY KEY (id),
    CONSTRAINT ck_chamados_prioridade CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_chamados_status CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'AGUARDANDO', 'RESOLVIDO', 'FECHADO')),
    -- Sem ON DELETE CASCADE: a exclusão de registros referenciados é bloqueada para preservar o histórico.
    CONSTRAINT fk_chamados_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_chamados_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuarios (id),
    CONSTRAINT fk_chamados_equipe FOREIGN KEY (equipe_id) REFERENCES equipes (id),
    CONSTRAINT fk_chamados_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_chamados_solicitante ON chamados (solicitante_id);
CREATE INDEX idx_chamados_responsavel ON chamados (responsavel_id);
CREATE INDEX idx_chamados_equipe_status ON chamados (equipe_id, status);
CREATE INDEX idx_chamados_categoria ON chamados (categoria_id);
CREATE INDEX idx_chamados_status ON chamados (status);
CREATE INDEX idx_chamados_prioridade ON chamados (prioridade);

-- Histórico somente de inserção: não há endpoints de edição ou exclusão.
CREATE TABLE historico_status_chamados (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    chamado_id      BIGINT      NOT NULL,
    status_anterior VARCHAR(20) NOT NULL,
    status_novo     VARCHAR(20) NOT NULL,
    data_alteracao  DATETIME(6) NOT NULL,
    CONSTRAINT pk_historico_status_chamados PRIMARY KEY (id),
    CONSTRAINT ck_historico_status_anterior CHECK (status_anterior IN ('ABERTO', 'EM_ANDAMENTO', 'AGUARDANDO', 'RESOLVIDO', 'FECHADO')),
    CONSTRAINT ck_historico_status_novo CHECK (status_novo IN ('ABERTO', 'EM_ANDAMENTO', 'AGUARDANDO', 'RESOLVIDO', 'FECHADO')),
    CONSTRAINT fk_historico_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_historico_chamado_data ON historico_status_chamados (chamado_id, data_alteracao);
