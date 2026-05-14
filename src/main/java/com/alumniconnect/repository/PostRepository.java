package com.alumniconnect.repository;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.Post;
import com.alumniconnect.entity.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findBySchoolIdAndStatus(Long schoolId, ApprovalStatus status);

    List<Post> findByBatchIdAndStatus(Long batchId, ApprovalStatus status);

    List<Post> findByBatchIdAndTypeAndStatus(Long batchId, PostType type, ApprovalStatus status);

    List<Post> findBySchoolIdAndBatchIsNullAndTypeAndStatus(Long schoolId, PostType type, ApprovalStatus status);

    long countByCreatedByIdAndStatus(Long createdById, ApprovalStatus status);

    List<Post> findByBatchIdAndTypeAndStatusAndEventDateGreaterThanEqual(
            Long batchId, PostType type, ApprovalStatus status, LocalDateTime eventDate
    );

    List<Post> findBySchoolIdAndBatchIsNullAndTypeAndStatusAndEventDateGreaterThanEqual(
            Long schoolId, PostType type, ApprovalStatus status, LocalDateTime eventDate
    );
}
