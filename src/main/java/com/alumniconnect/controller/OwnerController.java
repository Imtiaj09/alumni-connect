package com.alumniconnect.controller;

import com.alumniconnect.dto.CreateSchoolForm;
import com.alumniconnect.entity.ActivityLog;
import com.alumniconnect.entity.PlatformSettings;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.School;
import com.alumniconnect.entity.SubscriptionPlan;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.SchoolRepository;
import com.alumniconnect.repository.UserRepository;
import com.alumniconnect.service.ActivityLogService;
import com.alumniconnect.service.AnalyticsService;
import com.alumniconnect.service.FileStorageService;
import com.alumniconnect.service.PlatformService;
import com.alumniconnect.service.SchoolService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('SOFTWARE_OWNER')")
public class OwnerController {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final SchoolService schoolService;
    private final AnalyticsService analyticsService;
    private final ActivityLogService activityLogService;
    private final PlatformService platformService;
    private final FileStorageService fileStorageService;

    public OwnerController(SchoolRepository schoolRepository,
                           UserRepository userRepository,
                           SchoolService schoolService,
                           AnalyticsService analyticsService,
                           ActivityLogService activityLogService,
                           PlatformService platformService,
                           FileStorageService fileStorageService) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.schoolService = schoolService;
        this.analyticsService = analyticsService;
        this.activityLogService = activityLogService;
        this.platformService = platformService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<School> schools = schoolRepository.findAll();
        long activeSchools = schools.stream().filter(School::isActive).count();
        long inactiveSchools = schools.size() - activeSchools;
        long totalAdmins = userRepository.countByRole(Role.SCHOOL_ADMIN);
        long totalMembers = userRepository.countByRole(Role.MEMBER) + userRepository.countByRole(Role.BATCH_CONTROLLER);

        model.addAttribute("schools", schools);
        model.addAttribute("totalSchools", schools.size());
        model.addAttribute("activeSchools", activeSchools);
        model.addAttribute("inactiveSchools", inactiveSchools);
        model.addAttribute("totalAdmins", totalAdmins);
        model.addAttribute("totalMembers", totalMembers);
        return "owner/dashboard";
    }

    @GetMapping("/school/create")
    public String createSchoolForm(Model model) {
        model.addAttribute("form", new CreateSchoolForm());
        return "owner/create-school";
    }

    @PostMapping("/school/create")
    public String createSchool(@ModelAttribute("form") CreateSchoolForm form,
                               RedirectAttributes redirectAttributes) {
        try {
            if (form.getLogoFile() != null && !form.getLogoFile().isEmpty()) {
                String logoName = fileStorageService.storeFile(form.getLogoFile(), "logos");
                form.setLogoUrl("/uploads/logos/" + logoName);
            }
            if (form.getDocumentFile() != null && !form.getDocumentFile().isEmpty()) {
                String documentName = fileStorageService.storeFile(form.getDocumentFile(), "partnerships");
                form.setPartnershipDocumentUrl("/uploads/partnerships/" + documentName);
            }
        } catch (IOException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "ফাইল আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/owner/school/create";
        }

        School school = schoolService.createSchoolWithAdmin(form);
        logAction("SCHOOL_CREATED", "School", school.getId(), "School created: " + school.getName());
        redirectAttributes.addFlashAttribute("message", "স্কুল সফলভাবে তৈরি হয়েছে");
        return "redirect:/owner/dashboard";
    }

    @PostMapping("/school/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        School school = schoolService.toggleActive(id);
        String action = school.isActive() ? "SCHOOL_ACTIVATED" : "SCHOOL_DEACTIVATED";
        logAction(action, "School", school.getId(), "School status changed: " + school.getName());
        redirectAttributes.addFlashAttribute("message", "স্কুলের স্ট্যাটাস পরিবর্তন করা হয়েছে");
        return "redirect:/owner/dashboard";
    }

    @GetMapping("/school/{id}/admins")
    public String viewAdmins(@PathVariable Long id, Model model) {
        School school = schoolRepository.findById(id).orElseThrow(() -> new RuntimeException("School not found"));
        List<User> admins = userRepository.findBySchoolIdAndRole(id, Role.SCHOOL_ADMIN);
        model.addAttribute("school", school);
        model.addAttribute("admins", admins);
        return "owner/admins";
    }

    @PostMapping("/school/{id}/admin/add")
    public String addAdmin(@PathVariable Long id,
                           @RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {
        User admin = schoolService.addAdminToSchool(id, fullName, email, password);
        logAction("SCHOOL_ADMIN_ADDED", "User", admin.getId(), "Admin added: " + admin.getEmail());
        redirectAttributes.addFlashAttribute("message", "নতুন অ্যাডমিন যোগ করা হয়েছে");
        return "redirect:/owner/school/" + id + "/admins";
    }

    @GetMapping("/admins")
    public String allAdmins(Model model) {
        model.addAttribute("admins", userRepository.findByRole(Role.SCHOOL_ADMIN));
        return "owner/all-admins";
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("totalSchools", analyticsService.getTotalSchools());
        model.addAttribute("totalMembers", analyticsService.getTotalMembers());
        model.addAttribute("schoolsThisMonth", analyticsService.getSchoolsCreatedThisMonth());
        model.addAttribute("membersThisWeek", analyticsService.getMembersRegisteredThisWeek());
        model.addAttribute("totalDonations", analyticsService.getTotalDonations());
        return "owner/analytics";
    }

    @GetMapping("/activity-logs")
    public String activityLogs(Model model) {
        model.addAttribute("logs", activityLogService.getAllLogs());
        return "owner/activity-logs";
    }

    @GetMapping("/partnerships")
    public String partnerships(Model model) {
        model.addAttribute("schools", schoolRepository.findByPartnershipApprovedFalse());
        return "owner/partnerships";
    }

    @PostMapping("/partnership/approve/{schoolId}")
    public String approvePartnership(@PathVariable Long schoolId, RedirectAttributes redirect) {
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setPartnershipApproved(true);
        schoolRepository.save(school);
        logAction("PARTNERSHIP_APPROVED", "School", schoolId, "Partnership approved for " + school.getName());
        redirect.addFlashAttribute("message", "পার্টনারশিপ অনুমোদিত হয়েছে");
        return "redirect:/owner/partnerships";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Model model) {
        model.addAttribute("schools", schoolRepository.findAll());
        model.addAttribute("plans", SubscriptionPlan.values());
        return "owner/subscriptions";
    }

    @PostMapping("/subscription/update/{schoolId}")
    public String updateSubscription(@PathVariable Long schoolId,
                                     @RequestParam SubscriptionPlan plan,
                                     @RequestParam(required = false) String expiry,
                                     RedirectAttributes redirect) {
        School school = schoolRepository.findById(schoolId).orElseThrow(() -> new RuntimeException("School not found"));
        school.setSubscriptionPlan(plan);
        if (expiry != null && !expiry.isBlank()) {
            school.setSubscriptionExpiry(LocalDate.parse(expiry));
        } else {
            school.setSubscriptionExpiry(null);
        }
        schoolRepository.save(school);
        logAction("SUBSCRIPTION_UPDATED", "School", schoolId, "Subscription updated to " + plan);
        redirect.addFlashAttribute("message", "সাবস্ক্রিপশন আপডেট হয়েছে");
        return "redirect:/owner/subscriptions";
    }

    @GetMapping("/permissions")
    public String permissions(Model model) {
        model.addAttribute("settings", platformService.getSettings());
        return "owner/permissions";
    }

    @PostMapping("/permissions/update")
    public String updatePermissions(@ModelAttribute("settings") PlatformSettings settings,
                                    RedirectAttributes redirect) {
        platformService.updateSettings(settings);
        logAction("PLATFORM_PERMISSIONS_UPDATED", "PlatformSettings", 1L, "Global permissions updated");
        redirect.addFlashAttribute("message", "পারমিশন আপডেট হয়েছে");
        return "redirect:/owner/permissions";
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void logAction(String action, String entityType, Long entityId, String description) {
        activityLogService.log(new ActivityLog(action, entityType, entityId, description, currentUser()));
    }
}
