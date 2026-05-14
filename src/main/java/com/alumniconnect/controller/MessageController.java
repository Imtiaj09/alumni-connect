package com.alumniconnect.controller;

import com.alumniconnect.entity.Message;
import com.alumniconnect.entity.NotificationType;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import com.alumniconnect.service.MessageService;
import com.alumniconnect.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/member/messages")
@PreAuthorize("hasAnyRole('MEMBER','BATCH_CONTROLLER')")
public class MessageController {

    private final MessageService messageService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public MessageController(MessageService messageService,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.messageService = messageService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String inbox(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Message> inbox = messageService.getInbox(user.getId());
        model.addAttribute("currentUser", user);
        model.addAttribute("inbox", inbox);
        model.addAttribute("unreadCount", messageService.getUnreadCount(user.getId()));
        return "member/inbox";
    }

    @GetMapping("/chat/{userId}")
    public String chat(@PathVariable Long userId,
                       Model model,
                       @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        User otherUser = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (currentUser.getBatch() == null || otherUser.getBatch() == null
                || !currentUser.getBatch().getId().equals(otherUser.getBatch().getId())) {
            throw new RuntimeException("শুধু একই ব্যাচের সদস্যদের সাথে মেসেজ করা যাবে");
        }
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("otherUser", otherUser);
        model.addAttribute("messages", messageService.getConversation(currentUser.getId(), userId));
        return "member/chat";
    }

    @PostMapping("/chat/{userId}")
    public String send(@PathVariable Long userId,
                       @RequestParam String content,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirect) {
        User sender = getCurrentUser(userDetails);
        User receiver = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (sender.getBatch() == null || receiver.getBatch() == null
                || !sender.getBatch().getId().equals(receiver.getBatch().getId())) {
            throw new RuntimeException("শুধু একই ব্যাচের সদস্যদের সাথে মেসেজ করা যাবে");
        }
        if (content == null || content.isBlank()) {
            redirect.addFlashAttribute("error", "ফাঁকা মেসেজ পাঠানো যাবে না");
            return "redirect:/member/messages/chat/" + userId;
        }
        Message saved = messageService.send(sender, receiver, content.trim());
        notificationService.create(
                receiver,
                NotificationType.MESSAGE,
                sender.getFullName() + " আপনাকে একটি মেসেজ পাঠিয়েছে।",
                saved.getId()
        );
        return "redirect:/member/messages/chat/" + userId;
    }
}
