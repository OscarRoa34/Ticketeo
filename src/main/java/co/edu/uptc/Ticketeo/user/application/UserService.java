package co.edu.uptc.Ticketeo.user.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.user.domain.Role;
import co.edu.uptc.Ticketeo.user.domain.User;
import co.edu.uptc.Ticketeo.user.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<User> getAllUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
    }

    public User registerNewUser(String username, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
        }

        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.USER)
                .build();

        return userRepository.save(newUser);
    }
}
