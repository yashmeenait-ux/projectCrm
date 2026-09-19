package services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import entities.User;
import repositories.UserRepository;

@Service
public class Userservice {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Autowired
    private UserRepository userRepository;

    public void registerUserService(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Email is already registered.", ex);
        }
    }

    public boolean loginUserService(String email, String password) {

        User user = userRepository.findByEmail(email);

        if (user == null || password == null || user.getPassword() == null) {
            return false;
        }

        String storedPassword = user.getPassword();

        if (passwordEncoder.matches(password, storedPassword)) {
            return true;
        }

        // Backward compatibility for existing accounts stored as plaintext:
        // verify once, then transparently upgrade the stored password.
        if (!storedPassword.startsWith("$2a$")
                && !storedPassword.startsWith("$2b$")
                && !storedPassword.startsWith("$2y$")
                && password.equals(storedPassword)) {

            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
