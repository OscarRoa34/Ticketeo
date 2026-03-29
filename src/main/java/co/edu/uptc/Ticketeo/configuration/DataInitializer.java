package co.edu.uptc.Ticketeo.configuration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE eventos ALTER COLUMN id_categoria DROP NOT NULL");
            log.info("Columna eventos.id_categoria configurada para permitir null.");
        } catch (Exception e) {
            log.warn("No fue posible ajustar eventos.id_categoria a nullable automáticamente: {}", e.getMessage());
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("------------------------------------------------------");
            log.info("Usuario Administrador creado automáticamente.");
            log.info("Username: admin");
            log.info("Password: admin");
            log.info("------------------------------------------------------");
        } else {
            log.info("El usuario administrador ya existe en la base de datos.");
        }
        
    }
}
