package co.edu.uptc.Ticketeo.repository;

import co.edu.uptc.Ticketeo.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Busca un usuario por su nombre de usuario.
     * Este método es fundamental para la integración con Spring Security,
     * específicamente para la carga del usuario en el UserDetailsService.
     */
    Optional<User> findByUsername(String username);
}
