package com.alumniconnect.service;

import com.alumniconnect.entity.PlatformSettings;
import com.alumniconnect.repository.PlatformSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class PlatformService {

    private final PlatformSettingsRepository platformSettingsRepository;

    public PlatformService(PlatformSettingsRepository platformSettingsRepository) {
        this.platformSettingsRepository = platformSettingsRepository;
    }

    public PlatformSettings getSettings() {
        return platformSettingsRepository.findById(1L).orElseGet(() -> {
            PlatformSettings settings = new PlatformSettings();
            settings.setId(1L);
            return platformSettingsRepository.save(settings);
        });
    }

    public PlatformSettings updateSettings(PlatformSettings updated) {
        PlatformSettings settings = getSettings();
        settings.setMessagingEnabled(updated.isMessagingEnabled());
        settings.setGalleryEnabled(updated.isGalleryEnabled());
        settings.setDonationEnabled(updated.isDonationEnabled());
        return platformSettingsRepository.save(settings);
    }
}
