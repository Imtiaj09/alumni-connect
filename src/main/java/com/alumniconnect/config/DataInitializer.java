package com.alumniconnect.config;

import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String ownerEmail = "owner@alumniconnect.com";
        if (userRepository.findByEmail(ownerEmail).isEmpty()) {
            User owner = new User();
            owner.setFullName("Software Owner");
            owner.setEmail(ownerEmail);
            owner.setPassword(passwordEncoder.encode("admin123"));
            owner.setRole(Role.SOFTWARE_OWNER);
            owner.setEnabled(true);
            userRepository.save(owner);
            System.out.println("Default Software Owner created: " + ownerEmail);
        }
    }
}
