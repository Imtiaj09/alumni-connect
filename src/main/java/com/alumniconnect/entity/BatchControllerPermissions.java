package com.alumniconnect.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_controller_permissions")
public class BatchControllerPermissions {

    @Id
    private Long schoolId;

    private boolean canApprovePosts = false;
    private boolean canCreateNotices = true;
    private boolean canUploadGallery = false;
    private boolean canModerateMembers = false;

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public boolean isCanApprovePosts() {
        return canApprovePosts;
    }

    public void setCanApprovePosts(boolean canApprovePosts) {
        this.canApprovePosts = canApprovePosts;
    }

    public boolean isCanCreateNotices() {
        return canCreateNotices;
    }

    public void setCanCreateNotices(boolean canCreateNotices) {
        this.canCreateNotices = canCreateNotices;
    }

    public boolean isCanUploadGallery() {
        return canUploadGallery;
    }

    public void setCanUploadGallery(boolean canUploadGallery) {
        this.canUploadGallery = canUploadGallery;
    }

    public boolean isCanModerateMembers() {
        return canModerateMembers;
    }

    public void setCanModerateMembers(boolean canModerateMembers) {
        this.canModerateMembers = canModerateMembers;
    }
}

