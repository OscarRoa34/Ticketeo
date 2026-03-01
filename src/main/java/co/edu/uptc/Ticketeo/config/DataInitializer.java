package co.edu.uptc.Ticketeo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import co.edu.uptc.Ticketeo.models.Role;
import co.edu.uptc.Ticketeo.models.User;
import co.edu.uptc.Ticketeo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
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
