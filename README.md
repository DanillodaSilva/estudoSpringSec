# Help Desk API — estudo de Spring Security

API REST de gestão de chamados de suporte técnico, construída para servir de base prática ao estudo de **Spring Security**.

> ⚠️ **A segurança NÃO foi implementada — de propósito.** Todos os endpoints são públicos, não há login, perfis nem controle de acesso. IDs de solicitante e de autor (`solicitanteId`, `autorId`) são enviados pelo próprio cliente e **não comprovam identidade**: qualquer cliente pode informar qualquer ID. Use apenas localmente, com dados fictícios. O roteiro para proteger a API está em [SPRING_SECURITY_CHECKLIST.md](SPRING_SECURITY_CHECKLIST.md).

## Contexto do negócio

Usuários de uma organização abrem chamados (hardware, rede, acesso a sistemas…). Os chamados são atribuídos a equipes e a um responsável, recebem comentários e percorrem um ciclo de status com histórico preservado.

## Tecnologias

Java 21 · Spring Boot 4.1 (Web MVC, Data JPA, Validation) · Hibernate 7 · MySQL 8.4 · Flyway · Maven Wrapper · JUnit 5 · Mockito · Testcontainers.

## Estrutura

```text
src/main/java/br/com/estudos/helpdesk/
├── controller/          # endpoints REST (enxutos, só delegam)
├── service/             # regras de negócio e transações
├── repository/          # Spring Data JPA
├── entity/              # entidades JPA
├── dto/request|response # contratos da API (records + Bean Validation)
├── mapper/              # conversão DTO <-> entidade
├── model/enums/         # Prioridade, StatusChamado (com as transições)
└── exception/handler/   # GlobalExceptionHandler (Problem Details)
src/main/resources/db/
├── migration/           # V1..V4 — esquema versionado
└── dev-data/            # R__dados_ficticios.sql — só em desenvolvimento
```

## Entidades e relacionamentos

| Entidade | Relacionamentos |
|---|---|
| **Usuario** | pertence opcionalmente a uma Equipe (sem senha, perfis ou credenciais) |
| **Equipe** | possui vários Usuarios (membros) |
| **Categoria** | classifica Chamados |
| **Chamado** | solicitante (Usuario), responsável opcional (Usuario), Equipe opcional, Categoria |
| **Comentario** | autor (Usuario) e Chamado |
| **HistoricoStatus** | Chamado; somente inserção, sem edição/exclusão |

## Regras de negócio principais

- Chamado nasce **ABERTO**; título, descrição, prioridade, solicitante ativo e categoria ativa são obrigatórios.
- Transições de status permitidas (definidas em `StatusChamado`):

  | De | Para |
  |---|---|
  | ABERTO | EM_ANDAMENTO, AGUARDANDO, FECHADO |
  | EM_ANDAMENTO | AGUARDANDO, RESOLVIDO |
  | AGUARDANDO | EM_ANDAMENTO, RESOLVIDO |
  | RESOLVIDO | EM_ANDAMENTO (reabertura), FECHADO |
  | FECHADO | — (estado final) |

- Cada mudança de status gera histórico; ao fechar, `dataEncerramento` é preenchida automaticamente.
- Atribuição exige equipe ativa; o responsável (opcional) deve estar ativo e pertencer à equipe.
- Chamado **FECHADO** não aceita edição, atribuição, mudança de status nem comentários (novos, edição ou exclusão).
- Chamado só pode ser excluído se não tiver histórico nem comentários.
- E-mail único (sem diferenciar maiúsculas). Usuários com chamados/comentários não são excluídos: devem ser desativados.
- Usuário responsável por chamados ativos não pode trocar de equipe.
- Equipe com chamados (ativos ou encerrados) ou com membros não é excluída; categoria em uso também não.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET/POST | `/api/usuarios` | listar (paginado) / criar |
| GET/PUT/DELETE | `/api/usuarios/{id}` | consultar / atualizar (inclui `ativo`) / excluir |
| GET/POST | `/api/equipes` | listar / criar |
| GET/PUT/DELETE | `/api/equipes/{id}` | consultar (com membros) / atualizar / excluir |
| GET/POST | `/api/categorias` | listar / criar |
| GET/PUT/DELETE | `/api/categorias/{id}` | consultar / atualizar / excluir |
| GET/POST | `/api/chamados` | listar com filtros / abrir |
| GET/PUT/DELETE | `/api/chamados/{id}` | consultar / atualizar dados descritivos / excluir |
| PATCH | `/api/chamados/{id}/status` | alterar status `{"status": "EM_ANDAMENTO"}` |
| PATCH | `/api/chamados/{id}/atribuicao` | atribuir `{"equipeId": 1, "responsavelId": 3}` |
| GET | `/api/chamados/{id}/historico` | histórico de status |
| GET/POST | `/api/chamados/{id}/comentarios` | listar / comentar |
| GET/PUT/DELETE | `/api/chamados/{id}/comentarios/{comentarioId}` | consultar / editar / excluir |

**Listagens:** `?page=0&size=20&sort=campo,desc` (máx. 100 por página). Lista vazia retorna `200` com `conteudo: []`.
**Filtros de chamados:** `status`, `prioridade`, `categoriaId`, `responsavelId`, `equipeId`, `solicitanteId`.
**Erros** seguem Problem Details (RFC 9457): `400` validação/formato, `404` inexistente, `409` conflito de dados, `422` regra de negócio.

Exemplo — abrir um chamado:

```bash
curl -X POST http://localhost:8080/api/chamados -H "Content-Type: application/json" \
  -d '{"titulo":"Mouse com defeito","descricao":"Clique duplo involuntario","prioridade":"BAIXA","solicitanteId":1,"categoriaId":1}'
```

## Configurar o MySQL

1. Copie `.env.example` para `.env` e preencha `DB_PASSWORD` e `MYSQL_ROOT_PASSWORD` com valores seus (o `.env` não é versionado).
2. Suba o banco: `docker compose up -d` (MySQL 8.4 em `localhost:3307`, para não conflitar com um MySQL local na 3306).

Para usar outro MySQL, ajuste `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no `.env` ou como variáveis de ambiente. A aplicação lê o `.env` automaticamente.

## Migrations

O Flyway roda **automaticamente na inicialização**; o Hibernate apenas valida o esquema (`ddl-auto=validate`).
Os dados fictícios de `db/dev-data` são carregados por padrão em desenvolvimento; para desativá-los, defina `FLYWAY_LOCATIONS=classpath:db/migration`.
Novas alterações de esquema devem ser novas migrations (`V5__...sql`); nunca edite uma migration já aplicada.

## Executar

```bash
./mvnw spring-boot:run        # Linux/macOS/Git Bash
mvnw.cmd spring-boot:run      # Windows (PowerShell/CMD)
```

API em `http://localhost:8080`.

## Testes

```bash
./mvnw test     # unitários (services) e de controllers — não precisam de Docker
./mvnw verify   # + integração (*IT) com MySQL via Testcontainers — requer Docker em execução
```

Sem Docker, os testes de integração são ignorados (não falham).
