package co.edu.uptc.Ticketeo.user.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;
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

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean isProfileComplete(User user) {
        if (user == null) {
            return false;
        }
        return isNotBlank(user.getFirstName())
                && isNotBlank(user.getLastName())
                && user.getDocumentType() != null
                && isNotBlank(user.getDocumentNumber());
    }

    public User updateProfile(User user, String firstName, String lastName,
                              co.edu.uptc.Ticketeo.user.models.DocumentType documentType,
                              String documentNumber) {
        user.setFirstName(firstName == null ? null : firstName.trim());
        user.setLastName(lastName == null ? null : lastName.trim());
        user.setDocumentType(documentType);
        user.setDocumentNumber(documentNumber == null ? null : documentNumber.trim());
        return userRepository.save(user);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
