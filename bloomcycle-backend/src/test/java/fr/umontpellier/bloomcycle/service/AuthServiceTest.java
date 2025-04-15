package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.dto.auth.AuthResponse;
import fr.umontpellier.bloomcycle.dto.auth.LoginRequest;
import fr.umontpellier.bloomcycle.dto.auth.RegisterRequest;
import fr.umontpellier.bloomcycle.exception.EmailAlreadyExistsException;
import fr.umontpellier.bloomcycle.exception.InvalidCredentialsException;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, jwtService, passwordEncoder);
    }

    @Test
    void register_ShouldCreateNewUser_WhenValidRequest() {
        // Arrange
        RegisterRequest request = new RegisterRequest("test@test.com", "password", "testuser", "Test User");
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password("encodedPassword")
                .fullName(request.getFullName())
                .build();
        String token = "jwt.token.here";

        when(userService.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userService.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(token);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
        assertThat(response.getToken()).isEqualTo(token);
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("existing@test.com", "testuser", "password", "Test User");
        when(userService.existsByEmail(request.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void authenticate_ShouldReturnAuthResponse_WhenValidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("test@test.com", "password");
        User user = User.builder()
                .email(request.getEmail())
                .username("testuser")
                .password("encodedPassword")
                .build();
        String token = "jwt.token.here";

        when(userService.loadUserByUsername(request.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(token);

        // Act
        AuthResponse response = authService.authenticate(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getToken()).isEqualTo(token);
    }

    @Test
    void authenticate_ShouldThrowException_WhenInvalidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("test@test.com", "wrongpassword");
        User user = User.builder()
                .email(request.getEmail())
                .username("testuser")
                .password("encodedPassword")
                .build();

        when(userService.loadUserByUsername(request.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(request));
    }
}
