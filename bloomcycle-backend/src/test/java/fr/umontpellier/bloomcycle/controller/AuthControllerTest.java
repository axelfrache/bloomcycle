package fr.umontpellier.bloomcycle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.umontpellier.bloomcycle.dto.auth.AuthResponse;
import fr.umontpellier.bloomcycle.dto.auth.LoginRequest;
import fr.umontpellier.bloomcycle.dto.auth.RegisterRequest;
import fr.umontpellier.bloomcycle.exception.EmailAlreadyExistsException;
import fr.umontpellier.bloomcycle.exception.InvalidCredentialsException;
import fr.umontpellier.bloomcycle.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void register_ShouldReturnOk_WhenValidRequest() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.com")
                .password("password")
                .username("testuser")
                .fullName("Test User")
                .build();

        AuthResponse response = AuthResponse.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .token("jwt.token.here")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.username").value(request.getUsername()))
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailExists() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .password("password")
                .username("testuser")
                .fullName("Test User")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException(request.getEmail()));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .email("")  // Invalid email
                .password("password")
                .username("testuser")
                .fullName("Test User")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnOk_WhenValidCredentials() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@test.com")
                .password("password")
                .build();

        AuthResponse response = AuthResponse.builder()
                .email(request.getEmail())
                .username("testuser")
                .token("jwt.token.here")
                .build();

        when(authService.authenticate(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.getEmail()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("test@test.com")
                .password("wrongpassword")
                .build();

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturnBadRequest_WhenInvalidRequest() throws Exception {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .email("")  // Invalid email
                .password("password")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
