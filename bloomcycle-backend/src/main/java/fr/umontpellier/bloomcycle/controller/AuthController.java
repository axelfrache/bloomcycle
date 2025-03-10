package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.dto.auth.AuthResponse;
import fr.umontpellier.bloomcycle.dto.auth.LoginRequest;
import fr.umontpellier.bloomcycle.dto.auth.RegisterRequest;
import fr.umontpellier.bloomcycle.exception.EmailAlreadyExistsException;
import fr.umontpellier.bloomcycle.exception.InvalidCredentialsException;
import fr.umontpellier.bloomcycle.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.authenticate(request));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
} 