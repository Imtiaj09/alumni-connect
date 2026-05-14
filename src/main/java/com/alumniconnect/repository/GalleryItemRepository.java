package com.alumniconnect.repository;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {
    List<GalleryItem> findBySchoolIdAndStatus(Long schoolId, ApprovalStatus status);

    List<GalleryItem> findByBatchIdAndStatus(Long batchId, ApprovalStatus status);

    List<GalleryItem> findBySchoolIdAndStatusAndBatchIsNull(Long schoolId, ApprovalStatus status);

    long countByUploadedByIdAndStatus(Long uploadedById, ApprovalStatus status);
}
