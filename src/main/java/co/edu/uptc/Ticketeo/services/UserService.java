package co.edu.uptc.Ticketeo.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.models.Role;
import co.edu.uptc.Ticketeo.models.User;
import co.edu.uptc.Ticketeo.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un nuevo usuario con rol USER por defecto.
     * La contraseña se encripta antes de ser guardada en la base de datos.
     */
    public User registerNewUser(String username, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword)) // ¡Muy importante encriptar!
                .role(Role.USER) // Todos los registrados por la web son USER
                .build();

        return userRepository.save(newUser);
    }
}
