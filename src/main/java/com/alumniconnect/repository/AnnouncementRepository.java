package com.alumniconnect.repository;

import com.alumniconnect.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findBySchoolIdAndBatchIsNullOrderByCreatedAtDesc(Long schoolId);

    List<Announcement> findByBatchIdOrderByCreatedAtDesc(Long batchId);
}

