package br.com.estudos.helpdesk.repository;

import br.com.estudos.helpdesk.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByEquipeId(Long equipeId);
}
