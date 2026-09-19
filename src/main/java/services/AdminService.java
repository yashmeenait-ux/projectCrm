package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import entities.Admin;
import repositories.AdminRepository;

@Service
public class AdminService {

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Autowired
    private AdminRepository adminRepository;

    public boolean login(String username, String password) {

        Admin admin = adminRepository.findByUsername(username);

        if (admin == null || password == null || admin.getPassword() == null) {
            return false;
        }

        String storedPassword = admin.getPassword();

        if (passwordEncoder.matches(password, storedPassword)) {
            return true;
        }

        // Backward compatibility for existing plaintext admin accounts.
        if (!storedPassword.startsWith("$2a$")
                && !storedPassword.startsWith("$2b$")
                && !storedPassword.startsWith("$2y$")
                && password.equals(storedPassword)) {

            admin.setPassword(passwordEncoder.encode(password));
            adminRepository.save(admin);
            return true;
        }

        return false;
    }
}
