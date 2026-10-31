package br.com.estudos.helpdesk.repository;

import br.com.estudos.helpdesk.entity.Comentario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    Page<Comentario> findByChamadoId(Long chamadoId, Pageable pageable);

    Optional<Comentario> findByIdAndChamadoId(Long id, Long chamadoId);

    boolean existsByChamadoId(Long chamadoId);

    boolean existsByAutorId(Long autorId);
}
