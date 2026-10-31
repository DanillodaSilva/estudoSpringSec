-- Dados fictícios para testes manuais em desenvolvimento (não carregados no perfil de testes).
-- Migration repetível e idempotente: INSERT IGNORE com IDs fixos não duplica registros.
-- Nomes e e-mails são inventados (domínio reservado example.com). Nenhuma credencial é cadastrada.

INSERT IGNORE INTO equipes (id, nome, descricao, ativo, data_criacao, data_atualizacao) VALUES
    (1, 'Suporte N1', 'Primeiro atendimento ao usuário', TRUE, NOW(6), NOW(6)),
    (2, 'Infraestrutura', 'Redes, servidores e datacenter', TRUE, NOW(6), NOW(6)),
    (3, 'Sistemas Corporativos', 'ERP, CRM e sistemas internos', TRUE, NOW(6), NOW(6)),
    (4, 'Equipe Legada', 'Equipe desativada após reestruturação', FALSE, NOW(6), NOW(6));

INSERT IGNORE INTO categorias (id, nome, descricao, ativo, data_criacao, data_atualizacao) VALUES
    (1, 'Hardware', 'Computadores, periféricos e impressoras', TRUE, NOW(6), NOW(6)),
    (2, 'Software', 'Instalação e funcionamento de programas', TRUE, NOW(6), NOW(6)),
    (3, 'Rede', 'Conectividade, Wi-Fi e VPN', TRUE, NOW(6), NOW(6)),
    (4, 'Acesso a sistemas', 'Liberação e problemas de acesso', TRUE, NOW(6), NOW(6)),
    (5, 'Infraestrutura', 'Servidores, armazenamento e serviços internos', TRUE, NOW(6), NOW(6));

INSERT IGNORE INTO usuarios (id, nome, email, ativo, equipe_id, data_criacao, data_atualizacao) VALUES
    (1, 'Ana Souza', 'ana.souza@example.com', TRUE, NULL, NOW(6), NOW(6)),
    (2, 'Bruno Lima', 'bruno.lima@example.com', TRUE, NULL, NOW(6), NOW(6)),
    (3, 'Carla Mendes', 'carla.mendes@example.com', TRUE, 1, NOW(6), NOW(6)),
    (4, 'Diego Rocha', 'diego.rocha@example.com', TRUE, 1, NOW(6), NOW(6)),
    (5, 'Elisa Martins', 'elisa.martins@example.com', TRUE, 2, NOW(6), NOW(6)),
    (6, 'Fábio Nunes', 'fabio.nunes@example.com', TRUE, 3, NOW(6), NOW(6)),
    (7, 'Gabriela Torres', 'gabriela.torres@example.com', FALSE, 1, NOW(6), NOW(6));

INSERT IGNORE INTO chamados (id, titulo, descricao, prioridade, status, solicitante_id, responsavel_id, equipe_id,
                             categoria_id, data_abertura, data_atualizacao, data_encerramento) VALUES
    (1, 'Notebook não liga', 'O notebook não liga mesmo conectado à tomada.', 'ALTA', 'EM_ANDAMENTO',
        1, 3, 1, 1, NOW(6) - INTERVAL 3 DAY, NOW(6) - INTERVAL 2 DAY, NULL),
    (2, 'Sem acesso ao ERP', 'Mensagem de usuário bloqueado ao acessar o ERP.', 'CRITICA', 'AGUARDANDO',
        2, 6, 3, 4, NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 1 DAY, NULL),
    (3, 'Lentidão na rede do 2º andar', 'Conexão muito lenta em todas as estações do 2º andar.', 'MEDIA', 'FECHADO',
        1, 5, 2, 3, NOW(6) - INTERVAL 10 DAY, NOW(6) - INTERVAL 7 DAY, NOW(6) - INTERVAL 7 DAY),
    (4, 'Instalar editor de planilhas', 'Preciso do editor de planilhas instalado na minha estação.', 'BAIXA', 'ABERTO',
        2, NULL, NULL, 2, NOW(6) - INTERVAL 1 DAY, NOW(6) - INTERVAL 1 DAY, NULL);

INSERT IGNORE INTO historico_status_chamados (id, chamado_id, status_anterior, status_novo, data_alteracao) VALUES
    (1, 1, 'ABERTO', 'EM_ANDAMENTO', NOW(6) - INTERVAL 2 DAY),
    (2, 2, 'ABERTO', 'EM_ANDAMENTO', NOW(6) - INTERVAL 2 DAY),
    (3, 2, 'EM_ANDAMENTO', 'AGUARDANDO', NOW(6) - INTERVAL 1 DAY),
    (4, 3, 'ABERTO', 'EM_ANDAMENTO', NOW(6) - INTERVAL 9 DAY),
    (5, 3, 'EM_ANDAMENTO', 'RESOLVIDO', NOW(6) - INTERVAL 8 DAY),
    (6, 3, 'RESOLVIDO', 'FECHADO', NOW(6) - INTERVAL 7 DAY);

INSERT IGNORE INTO comentarios (id, conteudo, autor_id, chamado_id, data_criacao, data_atualizacao) VALUES
    (1, 'Verifiquei a fonte; vou testar com outro carregador.', 3, 1, NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY),
    (2, 'Obrigada! O notebook fica na mesa 12.', 1, 1, NOW(6) - INTERVAL 2 DAY, NOW(6) - INTERVAL 2 DAY),
    (3, 'Aguardando confirmação do gestor da área para liberar o acesso.', 6, 2, NOW(6) - INTERVAL 1 DAY, NOW(6) - INTERVAL 1 DAY),
    (4, 'Switch do 2º andar substituído; rede normalizada.', 5, 3, NOW(6) - INTERVAL 8 DAY, NOW(6) - INTERVAL 8 DAY);
