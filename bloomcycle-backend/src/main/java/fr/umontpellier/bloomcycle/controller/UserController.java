package fr.umontpellier.bloomcycle.controller;

import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        var createdUser = userService.registerUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestParam String email, @RequestParam String password) {
        var authenticatedUser = userService.loginUser(email, password);
        return ResponseEntity.ok(authenticatedUser);
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var email = authentication.getName();
        var user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
}