package com.alumniconnect.controller;

import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class NavContextAdvice {

    private final UserRepository userRepository;

    public NavContextAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("navSchoolLogoUrl")
    public String navSchoolLogoUrl(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null || user.getSchool() == null) {
            return null;
        }
        return user.getSchool().getLogoUrl();
    }

    @ModelAttribute("navSchoolName")
    public String navSchoolName(Authentication authentication) {
        User user = resolveCurrentUser(authentication);
        if (user == null || user.getSchool() == null) {
            return null;
        }
        return user.getSchool().getName();
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            return null;
        }
        return userRepository.findByEmail(username).orElse(null);
    }
}

