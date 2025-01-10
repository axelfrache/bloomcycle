package fr.umontpellier.bloomcycle.service;

import fr.umontpellier.bloomcycle.model.User;
import fr.umontpellier.bloomcycle.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    public User loginUser(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }

        var user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        return user;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    public User updateUser(Long userId, User updatedUser) {
        var user = getUserById(userId);

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());
        user.setFullName(updatedUser.getFullName());

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        var user = getUserById(userId);
        userRepository.delete(user);
    }
}