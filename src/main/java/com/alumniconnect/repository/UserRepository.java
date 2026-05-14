package com.alumniconnect.repository;

import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findBySchoolIdAndRole(Long schoolId, Role role);

    List<User> findBySchoolIdAndEnabledFalseAndRole(Long schoolId, Role role);

    List<User> findBySchoolIdAndEnabledTrueAndRoleIn(Long schoolId, List<Role> roles);

    List<User> findByBatchIdAndRole(Long batchId, Role role);

    List<User> findByBatchIdAndRoleIn(Long batchId, List<Role> roles);

    List<User> findByBatchIdAndEnabledFalseAndRoleIn(Long batchId, List<Role> roles);

    List<User> findByRole(Role role);

    long countByRole(Role role);

    long countBySchoolIdAndRole(Long schoolId, Role role);

    long countBySchoolIdAndEnabledFalseAndRole(Long schoolId, Role role);

    long countByCreatedAtAfter(LocalDateTime after);
}
