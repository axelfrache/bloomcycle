package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.repository.UserRepository;
import fr.umontpellier.bloomcycle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }
}