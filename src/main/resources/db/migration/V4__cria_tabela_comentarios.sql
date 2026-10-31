CREATE TABLE comentarios (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    conteudo         VARCHAR(2000) NOT NULL,
    autor_id         BIGINT        NOT NULL,
    chamado_id       BIGINT        NOT NULL,
    data_criacao     DATETIME(6)   NOT NULL,
    data_atualizacao DATETIME(6)   NOT NULL,
    CONSTRAINT pk_comentarios PRIMARY KEY (id),
    CONSTRAINT ck_comentarios_conteudo CHECK (CHAR_LENGTH(TRIM(conteudo)) > 0),
    CONSTRAINT fk_comentarios_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_comentarios_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_comentarios_autor ON comentarios (autor_id);
CREATE INDEX idx_comentarios_chamado_data ON comentarios (chamado_id, data_criacao);
