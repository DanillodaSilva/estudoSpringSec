# Checklist de aprendizado — Spring Security

Roteiro progressivo para proteger a Help Desk API. Cada item é um **desafio**: descreve o objetivo e um **critério de conclusão** verificável, mas não a solução. Faça os módulos em ordem; cada um depende dos anteriores.

Regras para você mesmo:

- Cada mudança de esquema é uma **nova migration** (`V5__...`, `V6__...`); não edite V1–V4.
- Antes de proteger algo, escreva (ou esboce) o teste que comprova a proteção.
- Mantenha `./mvnw verify` verde ao final de cada módulo e faça um commit por desafio concluído.

Pontos do código atual que você vai revisitar:

- `ChamadoRequest.solicitanteId` e `ComentarioRequest.autorId` — hoje o cliente informa quem ele é.
- `HistoricoStatus` — ainda não registra quem alterou o status.
- `Usuario` — entidade de domínio sem credenciais nem perfis.
- `GlobalExceptionHandler` — padroniza erros em Problem Details; 401 e 403 ainda não existem.

---

## Módulo 1 — Fundamentos

- [ ] **1.1 Entender a arquitetura.** Explique, com suas palavras, o caminho de uma requisição HTTP desde o servlet container até o controller quando o Spring Security está presente (filtros, contexto de segurança, autenticação, autorização).
  **Critério:** um parágrafo ou diagrama seu, salvo em `docs/` ou nas suas notas, citando ao menos cinco componentes e a ordem em que atuam.

- [ ] **1.2 Adicionar a dependência e observar.** Adicione o starter de segurança e suba a aplicação sem nenhuma configuração própria.
  **Critério:** você descreve o que mudou no comportamento padrão (quais endpoints passaram a ser bloqueados, que credencial foi gerada, onde ela aparece) e por que isso acontece.

- [ ] **1.3 Inspecionar a cadeia de filtros.** Descubra quais filtros estão ativos na sua aplicação e em que ordem.
  **Critério:** lista dos filtros ativos obtida a partir da própria aplicação (não da documentação).

- [ ] **1.4 Configurar sua própria SecurityFilterChain.** Substitua a configuração padrão por uma sua.
  **Critério:** a configuração padrão deixa de ser usada e a aplicação sobe sem a credencial gerada automaticamente.

- [ ] **1.5 Definir o que é público.** Decida quais rotas permanecem acessíveis sem autenticação (se houver alguma) e justifique.
  **Critério:** tabela rota × "pública/autenticada" no seu material de estudo, e a aplicação se comporta exatamente conforme a tabela.

- [ ] **1.6 Diferenciar 401 e 403.** Explique quando cada um deve ocorrer nesta API.
  **Critério:** dois exemplos concretos de requisição desta API, um que deve resultar em 401 e outro em 403, com a justificativa.

## Módulo 2 — Identidade e credenciais

- [ ] **2.1 Modelar credenciais.** Evolua a entidade `Usuario` para suportar autenticação, decidindo quais dados são necessários e quais nunca devem sair da API.
  **Critério:** nova migration aplicada, `ddl-auto=validate` continua passando e nenhum DTO de resposta expõe dados de credencial.

- [ ] **2.2 Armazenar senhas com segurança.** Garanta que a senha nunca seja persistida em texto puro.
  **Critério:** consultando o banco diretamente, não é possível ler nenhuma senha; dois usuários com a mesma senha não têm o mesmo valor armazenado.

- [ ] **2.3 Cadastrar e trocar senha.** Defina como um usuário recebe sua senha inicial e como a altera, sem quebrar o CRUD existente.
  **Critério:** fluxo documentado e testado; os dados fictícios de `db/dev-data` continuam carregando sem senhas fixas no repositório.

- [ ] **2.4 Carregar usuários do banco.** Faça o Spring Security buscar usuários na tabela `usuarios`.
  **Critério:** um usuário criado via `POST /api/usuarios` (e com senha definida) consegue se autenticar.

- [ ] **2.5 Respeitar o status do usuário.** Usuários inativos não podem se autenticar.
  **Critério:** teste demonstrando que, após `PUT /api/usuarios/{id}` com `"ativo": false`, as credenciais daquele usuário deixam de funcionar.

- [ ] **2.6 Tratar credenciais inválidas.** Defina a resposta para usuário inexistente, senha errada e usuário inativo.
  **Critério:** as três situações retornam o mesmo status e uma mensagem que não revela qual delas ocorreu.

## Módulo 3 — Perfis

- [ ] **3.1 Definir os perfis.** Descreva a responsabilidade de **ADMIN**, **GESTOR**, **ATENDENTE** e **SOLICITANTE** no contexto do help desk.
  **Critério:** matriz perfil × endpoint (incluindo os `PATCH` e os comentários) documentada antes de qualquer código.

- [ ] **3.2 Persistir perfis.** Decida se um usuário tem um ou vários perfis e modele isso no banco.
  **Critério:** migration aplicada e perfis associados aos usuários de `db/dev-data`.

- [ ] **3.3 Gerenciar perfis.** Defina como um perfil é concedido ou removido e quem pode fazer isso.
  **Critério:** um SOLICITANTE não consegue conceder perfis a si mesmo nem a ninguém (teste comprovando).

- [ ] **3.4 Hierarquia (opcional).** Avalie se algum perfil deve herdar as permissões de outro.
  **Critério:** decisão registrada com prós e contras; se adotada, testes comprovam a herança.

## Módulo 4 — Proteção de endpoints

- [ ] **4.1 Proteger a criação de equipes.** Apenas perfis administrativos criam, alteram e excluem equipes.
  **Critério:** `POST/PUT/DELETE /api/equipes` retornam 403 para ATENDENTE e SOLICITANTE e funcionam para os perfis autorizados pela sua matriz.

- [ ] **4.2 Restringir exclusão de usuários.** Somente o perfil definido na matriz pode excluir ou desativar usuários.
  **Critério:** testes de sucesso e de 403 para `DELETE /api/usuarios/{id}` e para a desativação via `PUT`.

- [ ] **4.3 Restringir atribuição de chamados.** Somente quem coordena o atendimento pode usar `PATCH /api/chamados/{id}/atribuicao`.
  **Critério:** SOLICITANTE recebe 403; o perfil autorizado consegue atribuir; as regras de negócio (422) continuam valendo para quem tem acesso.

- [ ] **4.4 Consultas conforme o perfil.** Defina quem pode listar usuários, equipes, categorias e chamados.
  **Critério:** cada listagem se comporta conforme a matriz, inclusive para um usuário autenticado sem nenhum perfil.

- [ ] **4.5 Acesso administrativo.** Garanta que exista ao menos um caminho para administrar o sistema e que ele não fique aberto.
  **Critério:** é impossível criar o primeiro ADMIN por uma requisição anônima.

- [ ] **4.6 Operações sensíveis.** Identifique as operações de maior impacto (exclusões, fechamento de chamados, mudanças de perfil) e restrinja-as.
  **Critério:** lista das operações sensíveis com o perfil exigido para cada uma, e um teste de 403 por operação.

## Módulo 5 — Autorização por recurso

- [ ] **5.1 Impedir acesso indevido a chamados.** Um usuário não deve ver chamados com os quais não tem relação.
  **Critério:** `GET /api/chamados/{id}` de um chamado alheio é negado para SOLICITANTE (decida e justifique se a resposta será 403 ou 404).

- [ ] **5.2 Solicitante vê os próprios chamados.** A listagem de chamados de um SOLICITANTE retorna apenas os dele, mesmo que ele envie outro `solicitanteId` no filtro.
  **Critério:** teste com dois solicitantes demonstrando o isolamento, inclusive com tentativa de manipular o filtro.

- [ ] **5.3 Atendente vê os chamados autorizados.** Defina a regra (ex.: chamados da sua equipe ou atribuídos a ele) e aplique-a em consultas, histórico e comentários.
  **Critério:** um ATENDENTE de outra equipe não consegue ler nem comentar o chamado.

- [ ] **5.4 Restringir edição de comentários.** Apenas o autor edita o próprio comentário; defina quem pode excluir.
  **Critério:** `PUT /api/chamados/{id}/comentarios/{comentarioId}` retorna 403 para quem não é o autor; as regras de negócio (chamado fechado → 422) continuam valendo.

- [ ] **5.5 Verificar propriedade de recursos.** Centralize as verificações de "este recurso pertence a este usuário?" sem espalhar a lógica pelos controllers.
  **Critério:** controllers continuam sem regras de acesso internas; as verificações de propriedade têm testes próprios.

- [ ] **5.6 Substituir IDs informados pelo cliente.** O solicitante de um chamado e o autor de um comentário passam a vir da identidade autenticada.
  **Critério:** `solicitanteId` e `autorId` deixam de ser aceitos (ou passam a ser ignorados de forma documentada); enviar o ID de outro usuário não tem efeito.

- [ ] **5.7 Registrar quem alterou o status.** O histórico passa a guardar o usuário responsável por cada mudança.
  **Critério:** nova migration; `GET /api/chamados/{id}/historico` exibe o autor de cada alteração; registros antigos continuam válidos.

## Módulo 6 — Estratégia de autenticação

- [ ] **6.1 Comparar estratégias.** Compare autenticação baseada em sessão e autenticação stateless com tokens para esta API: estado no servidor, revogação, CSRF, escalabilidade, logout e experiência do cliente.
  **Critério:** tabela comparativa com pelo menos seis critérios e sua conclusão.

- [ ] **6.2 Escolher e implementar uma estratégia.** Implemente **apenas uma** delas.
  **Critério:** um cliente HTTP (curl, Postman, Insomnia) consegue se autenticar e chamar endpoints protegidos; o fluxo está descrito no README.

- [ ] **6.3 Encerrar a autenticação.** Defina o que significa "logout" na estratégia escolhida e implemente.
  **Critério:** após o logout, a credencial/sessão anterior não dá mais acesso (ou a limitação é documentada e justificada).

- [ ] **6.4 Expiração.** Defina por quanto tempo uma autenticação é válida.
  **Critério:** teste ou demonstração de que uma autenticação expirada resulta em 401.

- [ ] **6.5 (Exercício posterior) A outra estratégia.** Implemente a estratégia que você não escolheu, em uma branch separada.
  **Critério:** suíte de testes de segurança passando nas duas branches.

## Módulo 7 — Segurança avançada

- [ ] **7.1 CSRF.** Decida se a proteção CSRF é necessária na estratégia escolhida e configure-a de acordo.
  **Critério:** justificativa escrita; se ativa, uma requisição de escrita sem o token esperado é rejeitada.

- [ ] **7.2 CORS.** Permita somente as origens que de fato precisam acessar a API.
  **Critério:** uma requisição de origem não permitida é bloqueada pelo navegador; nenhuma configuração usa curinga irrestrito com credenciais.

- [ ] **7.3 Falhas de autenticação.** Respostas 401 seguem o mesmo padrão Problem Details do restante da API.
  **Critério:** corpo do 401 com `status`, `title` e `detail`, sem stack trace nem detalhes internos.

- [ ] **7.4 Acesso negado.** Respostas 403 seguem o mesmo padrão.
  **Critério:** corpo do 403 no formato Problem Details, diferente do 401 e do 422.

- [ ] **7.5 Segurança em nível de método.** Aplique regras de acesso também na camada de serviço, onde fizer sentido.
  **Critério:** uma regra protegida no service continua valendo mesmo se o endpoint HTTP for exposto por engano (teste chamando o service diretamente).

- [ ] **7.6 Boas práticas de sessão ou token.** Revise os atributos e cuidados da estratégia escolhida (armazenamento no cliente, transporte, tempo de vida, renovação).
  **Critério:** checklist próprio com cada cuidado marcado como aplicado ou conscientemente não aplicado.

- [ ] **7.7 Proteção contra força bruta.** Limite tentativas de autenticação malsucedidas.
  **Critério:** após N falhas seguidas para a mesma conta, novas tentativas são recusadas por um período; um teste comprova o bloqueio e o desbloqueio.

- [ ] **7.8 Limitação de requisições.** Limite o volume de requisições por cliente nos endpoints mais sensíveis.
  **Critério:** ao exceder o limite, a API responde com o status HTTP apropriado para excesso de requisições.

- [ ] **7.9 Credenciais e segredos.** Nenhum segredo da aplicação (chaves, senhas de banco, segredos de assinatura) fica no código ou no repositório.
  **Critério:** busca no histórico Git não encontra segredos; a aplicação falha ao iniciar, com mensagem clara, se um segredo obrigatório não estiver configurado.

## Módulo 8 — Testes de segurança

- [ ] **8.1 Autenticação.** Teste o fluxo de autenticação bem-sucedido.
  **Critério:** teste automatizado que se autentica e acessa um endpoint protegido.

- [ ] **8.2 Acesso autorizado.** Para cada perfil, teste ao menos uma operação permitida.
  **Critério:** um teste por perfil retornando 2xx.

- [ ] **8.3 Acesso negado.** Para cada operação sensível do item 4.6, teste a negação.
  **Critério:** testes retornando 403 para todos os perfis não autorizados.

- [ ] **8.4 401 × 403.** Teste a diferença na prática.
  **Critério:** para o mesmo endpoint, requisição anônima → 401 e usuário autenticado sem permissão → 403.

- [ ] **8.5 Perfis.** Teste a matriz do item 3.1 de forma sistemática.
  **Critério:** testes parametrizados (perfil × endpoint × resultado esperado) cobrindo toda a matriz.

- [ ] **8.6 Propriedade de recursos.** Teste os cenários do Módulo 5.
  **Critério:** para cada regra de propriedade, um teste com o dono (sucesso) e outro com um terceiro (negado).

- [ ] **8.7 Credenciais inválidas.** Teste senha errada, usuário inexistente, usuário inativo e credencial expirada.
  **Critério:** quatro testes com respostas indistinguíveis entre si quanto ao motivo.

- [ ] **8.8 Tentativas de acesso indevido.** Teste manipulações típicas: alterar IDs na URL, enviar `solicitanteId`/`autorId` de terceiros, reutilizar credencial após logout, escalar o próprio perfil.
  **Critério:** todas as tentativas falham e nenhuma altera dados (verifique o estado no banco após cada uma).

- [ ] **8.9 Regressão.** Os testes existentes de regras de negócio continuam passando com a segurança ativa.
  **Critério:** `./mvnw verify` verde, sem desativar a segurança para fazer os testes antigos passarem.
