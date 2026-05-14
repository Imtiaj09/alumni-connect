package com.alumniconnect.service;

import com.alumniconnect.entity.Notification;
import com.alumniconnect.entity.NotificationType;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(User recipient, NotificationType type, String message, Long referenceId) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        return notificationRepository.save(notification);
    }

    public Notification create(User recipient, String message, NotificationType type, Long referenceId) {
        return create(recipient, type, message, referenceId);
    }

    public List<Notification> getForRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public long unreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long recipientId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getRecipient() != null && Objects.equals(notification.getRecipient().getId(), recipientId)) {
                notification.setRead(true);
            }
        });
    }

    @Transactional
    public void markAllRead(Long recipientId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        for (Notification notification : notifications) {
            notification.setRead(true);
        }
    }
}
