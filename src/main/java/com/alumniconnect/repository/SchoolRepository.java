package com.alumniconnect.repository;

import com.alumniconnect.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SchoolRepository extends JpaRepository<School, Long> {
    long countByCreatedAtAfter(LocalDateTime after);

    List<School> findByPartnershipApprovedFalse();
}
