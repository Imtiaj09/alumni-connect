package com.alumniconnect.service;

import com.alumniconnect.entity.Message;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message send(User sender, User receiver, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        return messageRepository.save(message);
    }

    @Transactional
    public List<Message> getConversation(Long currentUserId, Long otherUserId) {
        List<Message> messages = messageRepository.findConversation(currentUserId, otherUserId);
        for (Message message : messages) {
            if (message.getReceiver().getId().equals(currentUserId) && !message.isRead()) {
                message.setRead(true);
            }
        }
        return messages;
    }

    public List<Message> getInbox(Long userId) {
        return messageRepository.findLatestMessagesForUser(userId);
    }

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }
}

