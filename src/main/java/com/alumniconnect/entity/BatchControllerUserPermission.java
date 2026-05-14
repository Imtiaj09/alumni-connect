package com.alumniconnect.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "batch_controller_user_permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cr_permission_school_user", columnNames = {"school_id", "user_id"})
        }
)
public class BatchControllerUserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false)
    private Long schoolId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private boolean canApprovePosts = false;
    private boolean canCreateNotices = true;
    private boolean canUploadGallery = false;
    private boolean canModerateMembers = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

