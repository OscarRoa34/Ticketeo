package co.edu.uptc.Ticketeo.user.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<Long> findUserIdByUsername(@Param("username") String username);

    long countByRole(Role role);
}
