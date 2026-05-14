package com.alumniconnect.controller;

import com.alumniconnect.dto.RegistrationForm;
import com.alumniconnect.entity.Batch;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.School;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.BatchRepository;
import com.alumniconnect.repository.SchoolRepository;
import com.alumniconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class RegisterController {

    private final SchoolRepository schoolRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterController(SchoolRepository schoolRepository,
                              BatchRepository batchRepository,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder) {
        this.schoolRepository = schoolRepository;
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("schools", schoolRepository.findAll());
        model.addAttribute("batches", batchRepository.findAll());
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("registrationForm") RegistrationForm form) {
        if (form.getSchoolId() == null || form.getBatchId() == null
                || form.getFullName() == null || form.getEmail() == null || form.getPassword() == null
                || form.getFullName().isBlank() || form.getEmail().isBlank() || form.getPassword().isBlank()) {
            return "redirect:/register?error=invalid_form";
        }

        if (userRepository.existsByEmail(form.getEmail())) {
            return "redirect:/register?error=exists";
        }

        School school = schoolRepository.findById(form.getSchoolId())
                .orElseThrow(() -> new RuntimeException("স্কুল পাওয়া যায়নি"));
        Batch batch = batchRepository.findById(form.getBatchId())
                .orElseThrow(() -> new RuntimeException("ব্যাচ পাওয়া যায়নি"));

        if (batch.getSchool() == null || !school.getId().equals(batch.getSchool().getId())) {
            return "redirect:/register?error=batch_school_mismatch";
        }

        User user = new User();
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.MEMBER);
        user.setClassSection(form.getClassSection());
        user.setRollNumber(form.getRollNumber());
        user.setEnabled(false);
        user.setSchool(school);
        user.setBatch(batch);
        user.setBatchYear(batch.getBatchYear());
        userRepository.save(user);

        return "redirect:/register?success";
    }
}
