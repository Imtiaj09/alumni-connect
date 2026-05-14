package com.alumniconnect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String handleUploadException(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "ফাইল সাইজ বেশি। সর্বোচ্চ 20MB পর্যন্ত আপলোড করুন।");
        String referer = request.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            return "redirect:" + referer;
        }
        return "redirect:/";
    }
}
