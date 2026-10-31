package br.com.estudos.helpdesk.repository;

import br.com.estudos.helpdesk.entity.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {

    List<HistoricoStatus> findByChamadoIdOrderByDataAlteracaoAscIdAsc(Long chamadoId);

    boolean existsByChamadoId(Long chamadoId);
}
