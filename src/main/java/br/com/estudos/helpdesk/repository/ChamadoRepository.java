package br.com.estudos.helpdesk.repository;

import br.com.estudos.helpdesk.entity.Chamado;
import br.com.estudos.helpdesk.model.enums.Prioridade;
import br.com.estudos.helpdesk.model.enums.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    /**
     * Filtros opcionais: um parâmetro nulo não restringe o resultado.
     */
    @Query("""
            select c from Chamado c
            where (:status is null or c.status = :status)
              and (:prioridade is null or c.prioridade = :prioridade)
              and (:categoriaId is null or c.categoria.id = :categoriaId)
              and (:responsavelId is null or c.responsavel.id = :responsavelId)
              and (:equipeId is null or c.equipe.id = :equipeId)
              and (:solicitanteId is null or c.solicitante.id = :solicitanteId)
            """)
    Page<Chamado> filtrar(@Param("status") StatusChamado status,
                          @Param("prioridade") Prioridade prioridade,
                          @Param("categoriaId") Long categoriaId,
                          @Param("responsavelId") Long responsavelId,
                          @Param("equipeId") Long equipeId,
                          @Param("solicitanteId") Long solicitanteId,
                          Pageable pageable);

    boolean existsBySolicitanteIdOrResponsavelId(Long solicitanteId, Long responsavelId);

    boolean existsByResponsavelIdAndStatusNot(Long responsavelId, StatusChamado status);

    boolean existsByEquipeId(Long equipeId);

    boolean existsByEquipeIdAndStatusNot(Long equipeId, StatusChamado status);

    boolean existsByCategoriaId(Long categoriaId);
}
