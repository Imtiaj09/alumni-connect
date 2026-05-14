package com.alumniconnect.service;

import com.alumniconnect.entity.Role;
import com.alumniconnect.repository.DonationRepository;
import com.alumniconnect.repository.SchoolRepository;
import com.alumniconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AnalyticsService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;

    public AnalyticsService(SchoolRepository schoolRepository,
                            UserRepository userRepository,
                            DonationRepository donationRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
    }

    public long getTotalSchools() {
        return schoolRepository.count();
    }

    public long getTotalMembers() {
        return userRepository.countByRole(Role.MEMBER);
    }

    public long getSchoolsCreatedThisMonth() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        return schoolRepository.countByCreatedAtAfter(start);
    }

    public long getMembersRegisteredThisWeek() {
        LocalDateTime start = LocalDateTime.now().minusWeeks(1);
        return userRepository.countByCreatedAtAfter(start);
    }

    public BigDecimal getTotalDonations() {
        return donationRepository.getTotalDonations();
    }
}
