CREATE TABLE categorias (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    nome             VARCHAR(100) NOT NULL,
    descricao        VARCHAR(255) NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    data_criacao     DATETIME(6)  NOT NULL,
    data_atualizacao DATETIME(6)  NOT NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uk_categorias_nome UNIQUE (nome)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
