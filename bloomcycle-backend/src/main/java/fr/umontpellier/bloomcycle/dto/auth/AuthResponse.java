package fr.umontpellier.bloomcycle.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

@Data
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String username;
} 