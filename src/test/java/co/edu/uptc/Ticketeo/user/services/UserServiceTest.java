package co.edu.uptc.Ticketeo.user.services;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.uptc.Ticketeo.user.models.Role;
import co.edu.uptc.Ticketeo.user.models.User;
import co.edu.uptc.Ticketeo.user.repositorys.UserRepository;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerNewUser_whenUsernameExists_throwsException() {
        // Verifica que si el username ya existe,
        // el servicio lanza una excepción y no guarda el usuario.
        when(userRepository.findByUsername("oscar")).thenReturn(Optional.of(User.builder().id(1L).username("oscar").build()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerNewUser("oscar", "123456")
        );

        assertEquals("El nombre de usuario ya está en uso.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerNewUser_whenValid_encodesPasswordAndAssignsUserRole() {
        // Verifica que si el username no existe,
        // el servicio codifica la contraseña, asigna rol USER y guarda el usuario.
        when(userRepository.findByUsername("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

        User persisted = User.builder().id(8L).username("ana").password("encoded-pass").role(Role.USER).build();
        when(userRepository.save(any(User.class))).thenReturn(persisted);

        User result = userService.registerNewUser("ana", "plain-pass");

        assertEquals(8L, result.getId());
        assertEquals(Role.USER, result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        assertEquals("ana", toSave.getUsername());
        assertEquals("encoded-pass", toSave.getPassword());
        assertEquals(Role.USER, toSave.getRole());
    }

    @Test
    void getAllUsers_buildsPageRequestSortedByIdAscending() {
        // Verifica que el servicio construye correctamente el PageRequest
        // con paginación y orden ascendente por el campo "id".
        userService.getAllUsers(2, 15);

        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(userRepository).findAll(pageCaptor.capture());
        assertEquals(2, pageCaptor.getValue().getPageNumber());
        assertEquals(15, pageCaptor.getValue().getPageSize());
        assertTrue(pageCaptor.getValue().getSort().getOrderFor("id").isAscending());
    }
}