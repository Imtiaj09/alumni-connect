package com.alumniconnect.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = "/";

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_SOFTWARE_OWNER".equals(role)) {
                targetUrl = "/owner/dashboard";
                break;
            } else if ("ROLE_SCHOOL_ADMIN".equals(role)) {
                targetUrl = "/school/dashboard";
                break;
            } else if ("ROLE_BATCH_CONTROLLER".equals(role)) {
                targetUrl = "/batch/dashboard";
                break;
            } else if ("ROLE_MEMBER".equals(role)) {
                targetUrl = "/member/dashboard";
                break;
            }
        }

        response.sendRedirect(targetUrl);
    }
}
