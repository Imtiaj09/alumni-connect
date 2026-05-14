package com.alumniconnect.repository;

import com.alumniconnect.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findBySchoolId(Long schoolId);

    long countBySchoolId(Long schoolId);
}
