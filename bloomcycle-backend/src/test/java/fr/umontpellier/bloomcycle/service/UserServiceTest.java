package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.exception.ResourceNotFoundException;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void loadUserByUsername_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String email = "test@test.com";
        User user = User.builder()
                .email(email)
                .username("testuser")
                .password("password")
                .build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        User result = userService.loadUserByUsername(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        String email = "nonexistent@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(email));
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .username("testuser")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getUserById(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(userId));
        verify(userRepository).findById(userId);
    }

    @Test
    void updateUser_ShouldUpdateAndReturnUser_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User existingUser = User.builder()
                .id(userId)
                .email("old@test.com")
                .username("olduser")
                .fullName("Old Name")
                .build();

        User updatedUser = User.builder()
                .email("new@test.com")
                .username("newuser")
                .fullName("New Name")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.updateUser(userId, updatedUser);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(updatedUser.getEmail());
        assertThat(result.getUsername()).isEqualTo(updatedUser.getUsername());
        assertThat(result.getFullName()).isEqualTo(updatedUser.getFullName());
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Arrange
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@test.com")
                .username("testuser")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenUserExists() {
        // Arrange
        String email = "test@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenUserDoesNotExist() {
        // Arrange
        String email = "nonexistent@test.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        boolean result = userService.existsByEmail(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void save_ShouldReturnSavedUser() {
        // Arrange
        User user = User.builder()
                .email("test@test.com")
                .username("testuser")
                .password("password")
                .build();
        when(userRepository.save(user)).thenReturn(user);

        // Act
        User result = userService.save(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(user);
        verify(userRepository).save(user);
    }
}
