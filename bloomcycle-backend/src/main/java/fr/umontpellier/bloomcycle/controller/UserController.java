package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.service.UserService;
import fr.umontpellier.bloomcycle.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Hidden
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        try {
            log.info("Registering new user with email: {}", user.getEmail());

            if (user.getEmail() == null || user.getPassword() == null) {
                log.error("Missing required fields");
                return ResponseEntity.badRequest().build();
            }

            var registeredUser = userService.registerUser(user);
            log.info("User registered successfully: {}", registeredUser.getEmail());

            return ResponseEntity.ok(registeredUser);
        } catch (RuntimeException e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var email = authentication.getName();
        var user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}