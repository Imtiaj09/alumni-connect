package com.alumniconnect.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_settings")
public class PlatformSettings {

    @Id
    private Long id = 1L;

    private boolean messagingEnabled = true;
    private boolean galleryEnabled = true;
    private boolean donationEnabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isMessagingEnabled() {
        return messagingEnabled;
    }

    public void setMessagingEnabled(boolean messagingEnabled) {
        this.messagingEnabled = messagingEnabled;
    }

    public boolean isGalleryEnabled() {
        return galleryEnabled;
    }

    public void setGalleryEnabled(boolean galleryEnabled) {
        this.galleryEnabled = galleryEnabled;
    }

    public boolean isDonationEnabled() {
        return donationEnabled;
    }

    public void setDonationEnabled(boolean donationEnabled) {
        this.donationEnabled = donationEnabled;
    }
}
