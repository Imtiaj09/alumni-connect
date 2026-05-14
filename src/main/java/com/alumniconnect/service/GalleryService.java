package com.alumniconnect.service;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.GalleryItem;
import com.alumniconnect.repository.GalleryItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GalleryService {

    private final GalleryItemRepository galleryRepository;

    public GalleryService(GalleryItemRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    public GalleryItem upload(GalleryItem item) {
        return galleryRepository.save(item);
    }

    public List<GalleryItem> getApprovedBySchool(Long schoolId) {
        return galleryRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.APPROVED);
    }

    public List<GalleryItem> getApprovedByBatch(Long batchId) {
        return galleryRepository.findByBatchIdAndStatus(batchId, ApprovalStatus.APPROVED);
    }

    public GalleryItem approve(Long id) {
        GalleryItem item = galleryRepository.findById(id).orElseThrow(() -> new RuntimeException("Gallery item not found"));
        item.setStatus(ApprovalStatus.APPROVED);
        return galleryRepository.save(item);
    }

    public GalleryItem reject(Long id) {
        GalleryItem item = galleryRepository.findById(id).orElseThrow(() -> new RuntimeException("Gallery item not found"));
        item.setStatus(ApprovalStatus.REJECTED);
        return galleryRepository.save(item);
    }

    public void delete(Long id) {
        galleryRepository.deleteById(id);
    }
}
