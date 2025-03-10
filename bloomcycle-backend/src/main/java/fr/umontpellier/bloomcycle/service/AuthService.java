package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.dto.auth.AuthResponse;
import fr.umontpellier.bloomcycle.dto.auth.LoginRequest;
import fr.umontpellier.bloomcycle.dto.auth.RegisterRequest;
import fr.umontpellier.bloomcycle.exception.EmailAlreadyExistsException;
import fr.umontpellier.bloomcycle.exception.InvalidCredentialsException;
import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());
        
        if (userService.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        var user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();

        var savedUser = userService.save(user);
        var token = jwtService.generateToken(savedUser);

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse authenticate(LoginRequest request) {
        log.info("Authentication attempt for: {}", request.getEmail());
        
        var user = userService.loadUserByUsername(request.getEmail());
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new InvalidCredentialsException();

        var token = jwtService.generateToken(user);
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }
} 