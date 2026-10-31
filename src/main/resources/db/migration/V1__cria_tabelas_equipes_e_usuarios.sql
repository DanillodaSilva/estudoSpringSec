CREATE TABLE equipes (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nome             VARCHAR(100) NOT NULL,
    descricao        VARCHAR(255) NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    data_criacao     DATETIME(6)  NOT NULL,
    data_atualizacao DATETIME(6)  NOT NULL,
    CONSTRAINT pk_equipes PRIMARY KEY (id),
    CONSTRAINT uk_equipes_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Usuário é apenas uma entidade de domínio nesta etapa (sem credenciais ou perfis).
CREATE TABLE usuarios (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nome             VARCHAR(120) NOT NULL,
    email            VARCHAR(160) NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    equipe_id        BIGINT       NULL,
    data_criacao     DATETIME(6)  NOT NULL,
    data_atualizacao DATETIME(6)  NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_equipe FOREIGN KEY (equipe_id) REFERENCES equipes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_usuarios_equipe ON usuarios (equipe_id);
