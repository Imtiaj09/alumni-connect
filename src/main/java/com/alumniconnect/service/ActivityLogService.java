package com.alumniconnect.service;

import com.alumniconnect.entity.ActivityLog;
import com.alumniconnect.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(ActivityLog log) {
        activityLogRepository.save(log);
    }

    public List<ActivityLog> getAllLogs() {
        return activityLogRepository.findAllByOrderByTimestampDesc();
    }
}
