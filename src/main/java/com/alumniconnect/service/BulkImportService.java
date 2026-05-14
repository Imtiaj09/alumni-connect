package com.alumniconnect.service;

import com.alumniconnect.entity.Batch;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.BatchRepository;
import com.alumniconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class BulkImportService {

    private final UserRepository userRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    public BulkImportService(UserRepository userRepository,
                             BatchRepository batchRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.batchRepository = batchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public int importAlumni(MultipartFile file, Long batchId) throws IOException {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        int imported = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] data = trimmed.split(",");
                if (data.length < 2) {
                    continue;
                }

                String name = data[0].trim();
                String email = data[1].trim().toLowerCase();
                if (name.isEmpty() || email.isEmpty() || userRepository.existsByEmail(email)) {
                    continue;
                }

                User user = new User();
                user.setFullName(name);
                user.setEmail(email);
                user.setPhoneNumber(data.length > 2 ? data[2].trim() : "");
                user.setRollNumber(data.length > 3 ? data[3].trim() : "");
                user.setClassSection(data.length > 4 ? data[4].trim() : "");
                user.setBatch(batch);
                user.setSchool(batch.getSchool());
                user.setRole(Role.MEMBER);
                user.setEnabled(false);
                user.setPassword(passwordEncoder.encode("alumni123"));
                userRepository.save(user);
                imported++;
            }
        }

        return imported;
    }
}

