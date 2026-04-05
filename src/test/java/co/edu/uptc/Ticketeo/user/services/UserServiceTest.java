package co.edu.uptc.Ticketeo.user.services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerNewUser_validData_returnsSavedUser() {
        User savedUser = User.builder()
                .id(11L)
                .username("juan")
                .password("encoded-1234")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("juan")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("1234")).thenReturn("encoded-1234");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerNewUser("juan", "1234");

        assertEquals("juan", result.getUsername());
        assertEquals("encoded-1234", result.getPassword());
        assertEquals(Role.USER, result.getRole());
        verify(passwordEncoder).encode("1234");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerNewUser_existingUsername_throwsIllegalArgumentException() {
        User existingUser = User.builder().id(1L).username("juan").password("x").role(Role.USER).build();
        when(userRepository.findByUsername("juan")).thenReturn(Optional.of(existingUser));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.registerNewUser("juan", "1234"));

        assertTrue(exception.getMessage().contains("en uso"));
    }
}