package fr.umontpellier.bloomcycle.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterRequest {
    private String email;
    private String password;
    private String username;
    private String fullName;
} 