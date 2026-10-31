package br.com.estudos.helpdesk.repository;

import br.com.estudos.helpdesk.entity.Categoria;
import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.entity.Comentario;
import br.com.estudos.helpdesk.entity.Equipe;
import br.com.estudos.helpdesk.entity.HistoricoStatus;
import br.com.estudos.helpdesk.entity.Usuario;
import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import br.com.estudos.helpdesk.support.MySqlContainerConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida migrations, mapeamento JPA, relacionamentos, consultas e constraints contra MySQL real.
 * Cada teste roda em transação revertida ao final (@DataJpaTest).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(MySqlContainerConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class PersistenciaIT {

    @Autowired
    private Flyway flyway;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ChamadoRepository chamadoRepository;
    @Autowired
    private HistoricoStatusRepository historicoRepository;
    @Autowired
    private ComentarioRepository comentarioRepository;

    private Equipe suporte;
    private Categoria hardware;
    private Categoria rede;
    private Usuario solicitante;
    private Usuario atendente;

    @BeforeEach
    void prepararDados() {
        suporte = persistir(new Equipe("Suporte IT", "Equipe de teste"));
        hardware = persistir(new Categoria("Hardware IT", null));
        rede = persistir(new Categoria("Rede IT", null));
        solicitante = persistir(new Usuario("Solicitante", "solicitante.it@example.com", null));
        atendente = persistir(new Usuario("Atendente", "atendente.it@example.com", suporte));
    }

    @Test
    void deveAplicarTodasAsMigrationsVersionadasSemDadosFicticios() {
        MigrationInfo[] aplicadas = flyway.info().applied();

        assertThat(aplicadas).allMatch(m -> m.getState() == MigrationState.SUCCESS);
        assertThat(Arrays.stream(aplicadas).map(m -> m.getVersion().getVersion()))
                .containsExactly("1", "2", "3", "4");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void devePersistirERecarregarRelacionamentos() {
        Chamado chamado = new Chamado("Monitor piscando", "Imagem some a cada minuto", Prioridade.ALTA, solicitante, hardware);
        chamado.setEquipe(suporte);
        chamado.setResponsavel(atendente);
        chamado.setStatus(StatusChamado.EM_ANDAMENTO);
        persistir(chamado);
        persistir(new HistoricoStatus(chamado, StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO));
        persistir(new Comentario("Trocando o cabo", atendente, chamado));
        entityManager.flush();
        entityManager.clear();

        Chamado recarregado = chamadoRepository.findById(chamado.getId()).orElseThrow();
        assertThat(recarregado.getSolicitante().getNome()).isEqualTo("Solicitante");
        assertThat(recarregado.getResponsavel().getEquipe().getNome()).isEqualTo("Suporte IT");
        assertThat(recarregado.getCategoria().getNome()).isEqualTo("Hardware IT");
        assertThat(recarregado.getDataAbertura()).isNotNull();
        assertThat(historicoRepository.findByChamadoIdOrderByDataAlteracaoAscIdAsc(chamado.getId()))
                .singleElement()
                .satisfies(h -> assertThat(h.getStatusNovo()).isEqualTo(StatusChamado.EM_ANDAMENTO));
        assertThat(comentarioRepository.findByChamadoId(chamado.getId(), PageRequest.of(0, 10)).getContent())
                .extracting(Comentario::getConteudo).containsExactly("Trocando o cabo");

        Equipe equipe = entityManager.find(Equipe.class, suporte.getId());
        assertThat(equipe.getMembros()).extracting(Usuario::getNome).containsExactly("Atendente");
    }

    @Test
    void deveGarantirEmailUnicoSemDiferenciarMaiusculas() {
        assertThatThrownBy(() ->
                usuarioRepository.saveAndFlush(new Usuario("Duplicado", "SOLICITANTE.IT@EXAMPLE.COM", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveRejeitarStatusForaDoDominioNoBanco() {
        entityManager.flush();
        String sql = """
                INSERT INTO chamados (titulo, descricao, prioridade, status, solicitante_id, categoria_id,
                                      data_abertura, data_atualizacao)
                VALUES ('T', 'D', 'ALTA', 'INVALIDO', ?1, ?2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """;

        assertThatThrownBy(() -> entityManager.createNativeQuery(sql)
                .setParameter(1, solicitante.getId())
                .setParameter(2, hardware.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void deveImpedirExclusaoFisicaDeCategoriaReferenciada() {
        persistir(new Chamado("Sem rede", "Cabo desconectado", Prioridade.BAIXA, solicitante, rede));
        // Limpa o contexto para que a violação seja detectada pela FK do MySQL, e não pelo Hibernate em memória.
        entityManager.flush();
        entityManager.clear();

        categoriaRepository.delete(categoriaRepository.findById(rede.getId()).orElseThrow());

        assertThatThrownBy(() -> categoriaRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deveFiltrarChamadosPorCriteriosOpcionais() {
        Chamado semResponsavel = persistir(
                new Chamado("Teclado", "Teclas falhando", Prioridade.ALTA, solicitante, hardware));
        Chamado atribuido = new Chamado("Wi-Fi", "Sinal fraco", Prioridade.BAIXA, solicitante, rede);
        atribuido.setEquipe(suporte);
        atribuido.setResponsavel(atendente);
        atribuido.setStatus(StatusChamado.EM_ANDAMENTO);
        persistir(atribuido);
        var pagina = PageRequest.of(0, 10, Sort.by("titulo"));

        Page<Chamado> todos = chamadoRepository.filtrar(null, null, null, null, null, solicitante.getId(), pagina);
        assertThat(todos.getContent()).extracting(Chamado::getTitulo).containsExactly("Teclado", "Wi-Fi");

        assertThat(chamadoRepository.filtrar(StatusChamado.ABERTO, null, null, null, null, solicitante.getId(), pagina))
                .containsExactly(semResponsavel);
        assertThat(chamadoRepository.filtrar(null, null, null, atendente.getId(), null, null, pagina))
                .containsExactly(atribuido);
        assertThat(chamadoRepository.filtrar(null, Prioridade.ALTA, rede.getId(), null, null, null, pagina))
                .isEmpty();
        assertThat(chamadoRepository.filtrar(null, null, null, null, suporte.getId(), null, pagina))
                .containsExactly(atribuido);
    }

    @Test
    void deveIdentificarChamadosAtivosDaEquipeEVinculosDoUsuario() {
        Chamado chamado = new Chamado("VPN", "Não conecta", Prioridade.MEDIA, solicitante, rede);
        chamado.setEquipe(suporte);
        chamado.setResponsavel(atendente);
        chamado.setStatus(StatusChamado.FECHADO);
        persistir(chamado);

        assertThat(chamadoRepository.existsByEquipeIdAndStatusNot(suporte.getId(), StatusChamado.FECHADO)).isFalse();
        assertThat(chamadoRepository.existsByEquipeId(suporte.getId())).isTrue();
        assertThat(chamadoRepository.existsBySolicitanteIdOrResponsavelId(atendente.getId(), atendente.getId()))
                .isTrue();
        assertThat(usuarioRepository.existsByEquipeId(suporte.getId())).isTrue();
    }

    private <T> T persistir(T entidade) {
        entityManager.persist(entidade);
        return entidade;
    }
}
