package com.alumniconnect.service;

import com.alumniconnect.dto.CreateSchoolForm;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.School;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.SchoolRepository;
import com.alumniconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SchoolService(SchoolRepository schoolRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public School createSchoolWithAdmin(CreateSchoolForm form) {
        if (userRepository.existsByEmail(form.getAdminEmail())) {
            throw new IllegalArgumentException("এই ইমেইল দিয়ে আগে থেকেই অ্যাকাউন্ট আছে");
        }

        School school = new School();
        school.setName(form.getSchoolName());
        school.setAddress(form.getAddress());
        school.setCity(form.getCity());
        school.setCountry(form.getCountry());
        school.setPhone(form.getPhone());
        school.setEmail(form.getEmail());
        school.setLogoUrl(form.getLogoUrl());
        school.setPartnershipDocumentUrl(form.getPartnershipDocumentUrl());
        school.setPartnershipApproved(false);
        school.setActive(true);
        school = schoolRepository.save(school);

        User admin = new User();
        admin.setFullName(form.getAdminName());
        admin.setEmail(form.getAdminEmail());
        admin.setPassword(passwordEncoder.encode(form.getAdminPassword()));
        admin.setRole(Role.SCHOOL_ADMIN);
        admin.setEnabled(true);
        admin.setSchool(school);
        userRepository.save(admin);

        return school;
    }

    @Transactional
    public School toggleActive(Long schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));
        school.setActive(!school.isActive());
        return schoolRepository.save(school);
    }

    public User addAdminToSchool(Long schoolId, String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("এই ইমেইল দিয়ে আগে থেকেই অ্যাকাউন্ট আছে");
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));

        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.SCHOOL_ADMIN);
        admin.setEnabled(true);
        admin.setSchool(school);
        return userRepository.save(admin);
    }

    @Transactional
    public School updateSchoolProfile(Long schoolId,
                                      String name,
                                      String logoUrl,
                                      String address,
                                      String phone,
                                      String email,
                                      String website,
                                      String primaryColor,
                                      String secondaryColor) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new RuntimeException("School not found"));
        school.setName(name);
        school.setLogoUrl(logoUrl);
        school.setAddress(address);
        school.setPhone(phone);
        school.setEmail(email);
        school.setWebsite(website);
        school.setPrimaryColor(primaryColor);
        school.setSecondaryColor(secondaryColor);
        return schoolRepository.save(school);
    }
}
