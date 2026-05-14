package com.alumniconnect.repository;

import com.alumniconnect.entity.BatchControllerUserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BatchControllerUserPermissionRepository extends JpaRepository<BatchControllerUserPermission, Long> {
    Optional<BatchControllerUserPermission> findBySchoolIdAndUserId(Long schoolId, Long userId);

    List<BatchControllerUserPermission> findBySchoolId(Long schoolId);

    void deleteBySchoolIdAndUserId(Long schoolId, Long userId);
}

