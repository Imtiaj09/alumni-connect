package com.alumniconnect.controller;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.ActivityLog;
import com.alumniconnect.entity.Announcement;
import com.alumniconnect.entity.Batch;
import com.alumniconnect.entity.BatchControllerPermissions;
import com.alumniconnect.entity.BatchControllerUserPermission;
import com.alumniconnect.entity.Donation;
import com.alumniconnect.entity.FundCampaign;
import com.alumniconnect.entity.GalleryItem;
import com.alumniconnect.entity.NotificationType;
import com.alumniconnect.entity.Post;
import com.alumniconnect.entity.PostType;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.School;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.AnnouncementRepository;
import com.alumniconnect.repository.BatchRepository;
import com.alumniconnect.repository.BatchControllerPermissionsRepository;
import com.alumniconnect.repository.BatchControllerUserPermissionRepository;
import com.alumniconnect.repository.FundCampaignRepository;
import com.alumniconnect.repository.GalleryItemRepository;
import com.alumniconnect.repository.PostRepository;
import com.alumniconnect.repository.UserRepository;
import com.alumniconnect.service.BulkImportService;
import com.alumniconnect.service.FileStorageService;
import com.alumniconnect.service.FundCampaignService;
import com.alumniconnect.service.GalleryService;
import com.alumniconnect.service.MemberService;
import com.alumniconnect.service.NotificationService;
import com.alumniconnect.service.PostService;
import com.alumniconnect.service.SchoolService;
import com.alumniconnect.service.ActivityLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/school")
@PreAuthorize("hasRole('SCHOOL_ADMIN')")
public class AdminController {

    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final FundCampaignRepository fundCampaignRepository;
    private final GalleryItemRepository galleryItemRepository;
    private final AnnouncementRepository announcementRepository;
    private final BatchControllerPermissionsRepository batchControllerPermissionsRepository;
    private final BatchControllerUserPermissionRepository batchControllerUserPermissionRepository;
    private final MemberService memberService;
    private final PostService postService;
    private final FundCampaignService fundCampaignService;
    private final GalleryService galleryService;
    private final SchoolService schoolService;
    private final BulkImportService bulkImportService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;

    public AdminController(BatchRepository batchRepository,
                           UserRepository userRepository,
                           PostRepository postRepository,
                           FundCampaignRepository fundCampaignRepository,
                           GalleryItemRepository galleryItemRepository,
                           AnnouncementRepository announcementRepository,
                           BatchControllerPermissionsRepository batchControllerPermissionsRepository,
                           BatchControllerUserPermissionRepository batchControllerUserPermissionRepository,
                           MemberService memberService,
                           PostService postService,
                           FundCampaignService fundCampaignService,
                           GalleryService galleryService,
                           SchoolService schoolService,
                           BulkImportService bulkImportService,
                           NotificationService notificationService,
                           ActivityLogService activityLogService,
                           FileStorageService fileStorageService) {
        this.batchRepository = batchRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.fundCampaignRepository = fundCampaignRepository;
        this.galleryItemRepository = galleryItemRepository;
        this.announcementRepository = announcementRepository;
        this.batchControllerPermissionsRepository = batchControllerPermissionsRepository;
        this.batchControllerUserPermissionRepository = batchControllerUserPermissionRepository;
        this.memberService = memberService;
        this.postService = postService;
        this.fundCampaignService = fundCampaignService;
        this.galleryService = galleryService;
        this.schoolService = schoolService;
        this.bulkImportService = bulkImportService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.fileStorageService = fileStorageService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Long getSchoolId(UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getSchool() == null) {
            throw new RuntimeException("School assignment not found for current admin");
        }
        return user.getSchool().getId();
    }

    private School getCurrentSchool(UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getSchool() == null) {
            throw new RuntimeException("School assignment not found for current admin");
        }
        return user.getSchool();
    }

    private User getManageableMember(Long schoolId, Long memberId) {
        User member = userRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        if (member.getSchool() == null || !schoolId.equals(member.getSchool().getId())) {
            throw new RuntimeException("Unauthorized member operation");
        }
        if (member.getRole() != Role.MEMBER && member.getRole() != Role.BATCH_CONTROLLER) {
            throw new RuntimeException("This account is not manageable as school member");
        }
        return member;
    }

    private BatchControllerPermissions getDefaultCrPermissions(Long schoolId) {
        return batchControllerPermissionsRepository.findById(schoolId)
                .orElseGet(() -> {
                    BatchControllerPermissions p = new BatchControllerPermissions();
                    p.setSchoolId(schoolId);
                    return p;
                });
    }

    private User getPermissionTargetMember(Long schoolId, Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Member not found"));
        if (user.getSchool() == null || !schoolId.equals(user.getSchool().getId())) {
            throw new RuntimeException("Unauthorized member operation");
        }
        if (user.getRole() != Role.MEMBER && user.getRole() != Role.BATCH_CONTROLLER) {
            throw new RuntimeException("Invalid permission target");
        }
        return user;
    }

    private Post getSchoolPost(Long schoolId, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (post.getSchool() == null || !schoolId.equals(post.getSchool().getId())) {
            throw new RuntimeException("Unauthorized post operation");
        }
        return post;
    }

    private GalleryItem getSchoolGalleryItem(Long schoolId, Long galleryItemId) {
        GalleryItem item = galleryItemRepository.findById(galleryItemId)
                .orElseThrow(() -> new RuntimeException("Gallery item not found"));
        if (item.getSchool() == null || !schoolId.equals(item.getSchool().getId())) {
            throw new RuntimeException("Unauthorized gallery operation");
        }
        return item;
    }

    private void logAction(User actor, String action, String entityType, Long entityId, String description) {
        activityLogService.log(new ActivityLog(action, entityType, entityId, description, actor));
    }

    private int campaignProgressPercent(FundCampaign campaign) {
        if (campaign.getTargetAmount() == null || campaign.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal collected = campaign.getCollectedAmount() == null ? BigDecimal.ZERO : campaign.getCollectedAmount();
        BigDecimal ratio = collected
                .multiply(BigDecimal.valueOf(100))
                .divide(campaign.getTargetAmount(), 0, RoundingMode.HALF_UP);
        return Math.max(0, Math.min(100, ratio.intValue()));
    }

    private long campaignDaysRemaining(FundCampaign campaign) {
        if (campaign.getDeadline() == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), campaign.getDeadline());
    }

    private boolean isCampaignAtRisk(FundCampaign campaign) {
        if (campaign.getDeadline() == null || campaign.getTargetAmount() == null
                || campaign.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        LocalDate startDate = campaign.getCreatedAt() == null
                ? LocalDate.now()
                : campaign.getCreatedAt().toLocalDate();
        LocalDate endDate = campaign.getDeadline();
        if (endDate.isBefore(startDate)) {
            return false;
        }
        long totalDays = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate) + 1);
        long elapsed = Math.max(1, ChronoUnit.DAYS.between(startDate, LocalDate.now()) + 1);
        BigDecimal expectedRatio = BigDecimal.valueOf(Math.min(1.0, elapsed / (double) totalDays));
        BigDecimal collected = campaign.getCollectedAmount() == null ? BigDecimal.ZERO : campaign.getCollectedAmount();
        BigDecimal actualRatio = collected.divide(campaign.getTargetAmount(), 4, RoundingMode.HALF_UP);
        return actualRatio.compareTo(expectedRatio.subtract(BigDecimal.valueOf(0.12))) < 0;
    }

    private String formatPendingSla(long oldestPendingMinutes) {
        if (oldestPendingMinutes <= 0) {
            return "Fresh queue";
        }
        long days = oldestPendingMinutes / (60 * 24);
        long hours = (oldestPendingMinutes % (60 * 24)) / 60;
        long mins = oldestPendingMinutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        List<Role> memberRoles = Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER);
        List<Batch> schoolBatches = batchRepository.findBySchoolId(schoolId);
        List<User> approvedMembers = userRepository.findBySchoolIdAndEnabledTrueAndRoleIn(schoolId, memberRoles);
        List<User> pendingMembersList = memberService.getPendingMembersBySchool(schoolId);
        List<Post> pendingPostsList = postService.getPendingPostsBySchool(schoolId);
        List<GalleryItem> pendingGalleryList = galleryItemRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.PENDING);
        List<Post> approvedSchoolPosts = postRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.APPROVED);
        List<FundCampaign> activeCampaignList = fundCampaignRepository.findBySchoolIdAndClosedFalse(schoolId);

        long pendingMembers = pendingMembersList.size();
        long totalBatches = schoolBatches.size();
        long pendingPosts = pendingPostsList.size();
        long pendingGallery = pendingGalleryList.size();
        long totalMembers = approvedMembers.size();
        long activeCampaigns = activeCampaignList.size();
        long newMembers7d = approvedMembers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(last7Days))
                .count();
        long verifiedMembers = approvedMembers.stream().filter(User::isVerified).count();
        long suspendedMembers = approvedMembers.stream().filter(User::isSuspended).count();
        long activeMembers = approvedMembers.stream().filter(u -> !u.isSuspended()).count();

        List<Post> upcomingEvents = approvedSchoolPosts.stream()
                .filter(p -> p.getType() == PostType.EVENT && p.getEventDate() != null && !p.getEventDate().isBefore(now))
                .sorted(Comparator.comparing(Post::getEventDate))
                .toList();
        long upcomingEventsCount = upcomingEvents.size();
        List<Post> upcomingEventsPreview = upcomingEvents.stream().limit(5).toList();

        long campaignAtRiskCount = activeCampaignList.stream().filter(this::isCampaignAtRisk).count();
        BigDecimal activeCampaignRaised = activeCampaignList.stream()
                .map(c -> c.getCollectedAmount() == null ? BigDecimal.ZERO : c.getCollectedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> campaignRiskRows = activeCampaignList.stream()
                .sorted(Comparator.comparing(FundCampaign::getCreatedAt).reversed())
                .limit(6)
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", c.getId());
                    row.put("title", c.getTitle());
                    row.put("progress", campaignProgressPercent(c));
                    row.put("raised", c.getCollectedAmount() == null ? BigDecimal.ZERO : c.getCollectedAmount());
                    row.put("target", c.getTargetAmount() == null ? BigDecimal.ZERO : c.getTargetAmount());
                    row.put("daysLeft", campaignDaysRemaining(c));
                    row.put("risk", isCampaignAtRisk(c));
                    return row;
                })
                .toList();

        List<Map<String, Object>> batchPerformance = new ArrayList<>();
        for (Batch batch : schoolBatches) {
            List<User> batchUsers = userRepository.findByBatchIdAndRoleIn(batch.getId(), memberRoles);
            long batchApproved = batchUsers.stream().filter(User::isEnabled).count();
            long batchPending = batchUsers.size() - batchApproved;
            long batchActive = batchUsers.stream().filter(User::isEnabled).filter(u -> !u.isSuspended()).count();
            long postVolume = postRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.APPROVED).size();
            long eventVolume = postRepository.findByBatchIdAndTypeAndStatus(batch.getId(), PostType.EVENT, ApprovalStatus.APPROVED).size();
            BigDecimal donationRaised = activeCampaignList.stream()
                    .filter(c -> c.getBatch() != null && c.getBatch().getId().equals(batch.getId()))
                    .map(c -> c.getCollectedAmount() == null ? BigDecimal.ZERO : c.getCollectedAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long healthScore = batchActive * 3 + postVolume * 2 + eventVolume * 2;
            Map<String, Object> row = new HashMap<>();
            row.put("batchName", batch.getName());
            row.put("members", batchApproved);
            row.put("pending", batchPending);
            row.put("active", batchActive);
            row.put("posts", postVolume);
            row.put("events", eventVolume);
            row.put("donationRaised", donationRaised);
            row.put("score", healthScore);
            batchPerformance.add(row);
        }
        batchPerformance.sort((a, b) -> Long.compare((Long) b.get("score"), (Long) a.get("score")));
        if (batchPerformance.size() > 6) {
            batchPerformance = new ArrayList<>(batchPerformance.subList(0, 6));
        }

        List<LocalDateTime> pendingTimestamps = new ArrayList<>();
        pendingMembersList.stream().map(User::getCreatedAt).filter(ts -> ts != null).forEach(pendingTimestamps::add);
        pendingPostsList.stream().map(Post::getCreatedAt).filter(ts -> ts != null).forEach(pendingTimestamps::add);
        pendingGalleryList.stream().map(GalleryItem::getCreatedAt).filter(ts -> ts != null).forEach(pendingTimestamps::add);

        long oldestPendingMinutes = pendingTimestamps.stream()
                .mapToLong(ts -> ChronoUnit.MINUTES.between(ts, now))
                .max()
                .orElse(0);
        long avgPendingMinutes = pendingTimestamps.isEmpty()
                ? 0
                : Math.round(
                pendingTimestamps.stream()
                        .mapToLong(ts -> ChronoUnit.MINUTES.between(ts, now))
                        .average()
                        .orElse(0)
        );

        long funnelRegistered = pendingMembers + totalMembers;
        long funnelApproved = totalMembers;
        long funnelVerified = verifiedMembers;
        long funnelActive = activeMembers;

        model.addAttribute("pendingMembers", pendingMembers);
        model.addAttribute("totalBatches", totalBatches);
        model.addAttribute("pendingPosts", pendingPosts);
        model.addAttribute("pendingGallery", pendingGallery);
        model.addAttribute("totalPendingQueue", pendingMembers + pendingPosts + pendingGallery);
        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("activeCampaigns", activeCampaigns);
        model.addAttribute("newMembers7d", newMembers7d);
        model.addAttribute("upcomingEventsCount", upcomingEventsCount);
        model.addAttribute("upcomingEventsPreview", upcomingEventsPreview);
        model.addAttribute("campaignAtRiskCount", campaignAtRiskCount);
        model.addAttribute("campaignRiskRows", campaignRiskRows);
        model.addAttribute("activeCampaignRaised", activeCampaignRaised);
        model.addAttribute("batchPerformance", batchPerformance);
        model.addAttribute("oldestPendingMinutes", oldestPendingMinutes);
        model.addAttribute("oldestPendingLabel", formatPendingSla(oldestPendingMinutes));
        model.addAttribute("avgPendingMinutes", avgPendingMinutes);
        model.addAttribute("funnelRegistered", funnelRegistered);
        model.addAttribute("funnelApproved", funnelApproved);
        model.addAttribute("funnelVerified", funnelVerified);
        model.addAttribute("funnelActive", funnelActive);
        model.addAttribute("verifiedMembers", verifiedMembers);
        model.addAttribute("suspendedMembers", suspendedMembers);
        return "school/dashboard";
    }

    @GetMapping("/batches")
    public String batches(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/batches";
    }

    @PostMapping("/batches/create")
    public String createBatch(@RequestParam String name,
                              @RequestParam Integer batchYear,
                              @RequestParam(required = false) String description,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirect) {
        User admin = getCurrentUser(userDetails);
        Batch batch = new Batch();
        batch.setName(name);
        batch.setBatchYear(batchYear);
        batch.setDescription(description);
        batch.setSchool(admin.getSchool());
        batchRepository.save(batch);
        logAction(admin, "SCHOOL_BATCH_CREATED", "Batch", batch.getId(), "Batch created: " + batch.getName());
        redirect.addFlashAttribute("message", "নতুন ব্যাচ তৈরি হয়েছে");
        return "redirect:/school/batches";
    }

    @PostMapping("/batches/delete/{id}")
    public String deleteBatch(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        Batch batch = batchRepository.findById(id).orElseThrow(() -> new RuntimeException("Batch not found"));
        if (batch.getSchool() == null || !schoolId.equals(batch.getSchool().getId())) {
            throw new RuntimeException("Unauthorized batch operation");
        }
        User admin = getCurrentUser(userDetails);
        batchRepository.delete(batch);
        logAction(admin, "SCHOOL_BATCH_DELETED", "Batch", id, "Batch deleted: " + batch.getName());
        redirect.addFlashAttribute("message", "ব্যাচ মুছে ফেলা হয়েছে");
        return "redirect:/school/batches";
    }

    @GetMapping("/members/pending")
    public String pendingMembers(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        List<User> pending = memberService.getPendingMembersBySchool(schoolId);
        model.addAttribute("pendingMembers", pending);
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/members-pending";
    }

    @PostMapping("/members/approve/{id}")
    public String approveMember(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, id);
        memberService.approveMember(id);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনার সদস্যপদ অনুমোদিত হয়েছে।",
                member.getId()
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_MEMBER_APPROVED",
                "User",
                member.getId(),
                "Member approved: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্য অনুমোদিত হয়েছে");
        return "redirect:/school/members/pending";
    }

    @PostMapping("/members/reject/{id}")
    public String rejectMember(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, id);
        memberService.rejectMember(id);
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_MEMBER_REJECTED",
                "User",
                member.getId(),
                "Member rejected: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্য বাতিল করা হয়েছে");
        return "redirect:/school/members/pending";
    }

    @PostMapping("/members/assign-cr")
    public String assignBatchController(@RequestParam Long memberId,
                                        @RequestParam Long batchId,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, memberId);
        Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));
        if (batch.getSchool() == null || !schoolId.equals(batch.getSchool().getId())) {
            throw new RuntimeException("Unauthorized batch operation");
        }
        memberService.assignBatchController(memberId, batchId);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনাকে ব্যাচ কন্ট্রোলার হিসেবে নিযুক্ত করা হয়েছে।",
                batchId
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_ASSIGNED",
                "User",
                member.getId(),
                "CR assigned: " + member.getEmail() + " for batch " + batch.getName()
        );
        redirect.addFlashAttribute("message", "ব্যাচ কন্ট্রোলার হিসেবে নিযুক্ত করা হয়েছে");
        return "redirect:/school/members/pending";
    }

    @GetMapping("/content-moderation")
    public String contentModeration(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        return moderationQueue("ALL", model, userDetails);
    }

    @GetMapping("/moderation")
    public String moderationQueue(@RequestParam(defaultValue = "ALL") String type,
                                  Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        List<User> pendingMembers = memberService.getPendingMembersBySchool(schoolId);
        List<Post> pendingPosts = postService.getPendingPostsBySchool(schoolId);
        List<GalleryItem> pendingGallery = galleryItemRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.PENDING);

        String normalizedType = type == null ? "ALL" : type.trim().toUpperCase(Locale.ROOT);
        if (!List.of("ALL", "MEMBERS", "POSTS", "GALLERY").contains(normalizedType)) {
            normalizedType = "ALL";
        }

        model.addAttribute("activeType", normalizedType);
        model.addAttribute("pendingMembers", pendingMembers);
        model.addAttribute("pendingPosts", postService.getPendingPostsBySchool(schoolId));
        model.addAttribute("pendingGallery", pendingGallery);
        model.addAttribute("totalPendingQueue", pendingMembers.size() + pendingPosts.size() + pendingGallery.size());
        return "school/moderation-queue";
    }

    @PostMapping("/content/approve/{id}")
    public String approveContent(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        Post post = getSchoolPost(schoolId, id);
        postService.approvePost(id);
        if (post.getCreatedBy() != null) {
            notificationService.create(
                    post.getCreatedBy(),
                    NotificationType.POST_APPROVED,
                    "আপনার পোস্ট অনুমোদিত হয়েছে।",
                    id
            );
        }
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_POST_APPROVED",
                "Post",
                id,
                "Post approved: " + post.getTitle()
        );
        redirect.addFlashAttribute("message", "পোস্ট অনুমোদিত হয়েছে");
        return "redirect:/school/moderation?type=POSTS";
    }

    @PostMapping("/content/reject/{id}")
    public String rejectContent(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        Post post = getSchoolPost(schoolId, id);
        postService.rejectPost(id);
        if (post.getCreatedBy() != null) {
            notificationService.create(
                    post.getCreatedBy(),
                    NotificationType.POST_REJECTED,
                    "আপনার পোস্ট প্রত্যাখ্যান করা হয়েছে।",
                    id
            );
        }
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_POST_REJECTED",
                "Post",
                id,
                "Post rejected: " + post.getTitle()
        );
        redirect.addFlashAttribute("message", "পোস্ট প্রত্যাখ্যান হয়েছে");
        return "redirect:/school/moderation?type=POSTS";
    }

    @PostMapping("/gallery/approve/{id}")
    public String approveGalleryItem(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        GalleryItem item = getSchoolGalleryItem(schoolId, id);
        galleryService.approve(id);
        if (item.getUploadedBy() != null) {
            notificationService.create(
                    item.getUploadedBy(),
                    NotificationType.POST_APPROVED,
                    "আপনার গ্যালারি আইটেম অনুমোদিত হয়েছে।",
                    id
            );
        }
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_GALLERY_APPROVED",
                "GalleryItem",
                id,
                "Gallery item approved"
        );
        redirect.addFlashAttribute("message", "গ্যালারি আইটেম অনুমোদিত হয়েছে");
        return "redirect:/school/moderation?type=GALLERY";
    }

    @PostMapping("/gallery/reject/{id}")
    public String rejectGalleryItem(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        GalleryItem item = getSchoolGalleryItem(schoolId, id);
        galleryService.reject(id);
        if (item.getUploadedBy() != null) {
            notificationService.create(
                    item.getUploadedBy(),
                    NotificationType.POST_REJECTED,
                    "আপনার গ্যালারি আইটেম প্রত্যাখ্যান করা হয়েছে।",
                    id
            );
        }
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_GALLERY_REJECTED",
                "GalleryItem",
                id,
                "Gallery item rejected"
        );
        redirect.addFlashAttribute("message", "গ্যালারি আইটেম প্রত্যাখ্যান হয়েছে");
        return "redirect:/school/moderation?type=GALLERY";
    }

    @PostMapping("/moderation/members/bulk")
    public String bulkMemberModeration(@RequestParam(name = "memberIds", required = false) List<Long> memberIds,
                                       @RequestParam String action,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        if (memberIds == null || memberIds.isEmpty()) {
            redirect.addFlashAttribute("message", "কোনো সদস্য নির্বাচন করা হয়নি");
            return "redirect:/school/moderation?type=MEMBERS";
        }

        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        int processed = 0;
        User actor = getCurrentUser(userDetails);

        for (Long memberId : memberIds) {
            User member = getManageableMember(schoolId, memberId);
            if ("APPROVE".equals(normalizedAction)) {
                memberService.approveMember(memberId);
                notificationService.create(member, NotificationType.APPROVAL, "আপনার সদস্যপদ অনুমোদিত হয়েছে।", memberId);
                logAction(actor, "SCHOOL_MEMBER_APPROVED", "User", memberId, "Bulk approval: " + member.getEmail());
                processed++;
            } else if ("REJECT".equals(normalizedAction)) {
                memberService.rejectMember(memberId);
                logAction(actor, "SCHOOL_MEMBER_REJECTED", "User", memberId, "Bulk rejection: " + member.getEmail());
                processed++;
            }
        }
        redirect.addFlashAttribute("message", "সদস্য মডারেশন সম্পন্ন: " + processed);
        return "redirect:/school/moderation?type=MEMBERS";
    }

    @PostMapping("/moderation/posts/bulk")
    public String bulkPostModeration(@RequestParam(name = "postIds", required = false) List<Long> postIds,
                                     @RequestParam String action,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        if (postIds == null || postIds.isEmpty()) {
            redirect.addFlashAttribute("message", "কোনো পোস্ট নির্বাচন করা হয়নি");
            return "redirect:/school/moderation?type=POSTS";
        }

        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        int processed = 0;
        User actor = getCurrentUser(userDetails);

        for (Long postId : postIds) {
            Post post = getSchoolPost(schoolId, postId);
            if ("APPROVE".equals(normalizedAction)) {
                postService.approvePost(postId);
                if (post.getCreatedBy() != null) {
                    notificationService.create(post.getCreatedBy(), NotificationType.POST_APPROVED, "আপনার পোস্ট অনুমোদিত হয়েছে।", postId);
                }
                logAction(actor, "SCHOOL_POST_APPROVED", "Post", postId, "Bulk post approval: " + post.getTitle());
                processed++;
            } else if ("REJECT".equals(normalizedAction)) {
                postService.rejectPost(postId);
                if (post.getCreatedBy() != null) {
                    notificationService.create(post.getCreatedBy(), NotificationType.POST_REJECTED, "আপনার পোস্ট প্রত্যাখ্যান করা হয়েছে।", postId);
                }
                logAction(actor, "SCHOOL_POST_REJECTED", "Post", postId, "Bulk post rejection: " + post.getTitle());
                processed++;
            }
        }
        redirect.addFlashAttribute("message", "পোস্ট মডারেশন সম্পন্ন: " + processed);
        return "redirect:/school/moderation?type=POSTS";
    }

    @PostMapping("/moderation/gallery/bulk")
    public String bulkGalleryModeration(@RequestParam(name = "galleryIds", required = false) List<Long> galleryIds,
                                        @RequestParam String action,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        if (galleryIds == null || galleryIds.isEmpty()) {
            redirect.addFlashAttribute("message", "কোনো গ্যালারি আইটেম নির্বাচন করা হয়নি");
            return "redirect:/school/moderation?type=GALLERY";
        }

        String normalizedAction = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        int processed = 0;
        User actor = getCurrentUser(userDetails);

        for (Long galleryId : galleryIds) {
            GalleryItem item = getSchoolGalleryItem(schoolId, galleryId);
            if ("APPROVE".equals(normalizedAction)) {
                galleryService.approve(galleryId);
                if (item.getUploadedBy() != null) {
                    notificationService.create(item.getUploadedBy(), NotificationType.POST_APPROVED, "আপনার গ্যালারি আইটেম অনুমোদিত হয়েছে।", galleryId);
                }
                logAction(actor, "SCHOOL_GALLERY_APPROVED", "GalleryItem", galleryId, "Bulk gallery approval");
                processed++;
            } else if ("REJECT".equals(normalizedAction)) {
                galleryService.reject(galleryId);
                if (item.getUploadedBy() != null) {
                    notificationService.create(item.getUploadedBy(), NotificationType.POST_REJECTED, "আপনার গ্যালারি আইটেম প্রত্যাখ্যান করা হয়েছে।", galleryId);
                }
                logAction(actor, "SCHOOL_GALLERY_REJECTED", "GalleryItem", galleryId, "Bulk gallery rejection");
                processed++;
            }
        }
        redirect.addFlashAttribute("message", "গ্যালারি মডারেশন সম্পন্ন: " + processed);
        return "redirect:/school/moderation?type=GALLERY";
    }

    @GetMapping("/fund-campaigns")
    public String listCampaigns(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("campaigns", fundCampaignRepository.findBySchoolIdAndClosedFalse(schoolId));
        return "school/fund-campaigns";
    }

    @GetMapping("/fund-campaign/create")
    public String createCampaignForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("campaign", new FundCampaign());
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/create-campaign";
    }

    @PostMapping("/fund-campaign/create")
    public String createCampaign(@ModelAttribute("campaign") FundCampaign campaign,
                                 @RequestParam(required = false) Long batchId,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirect) {
        User admin = getCurrentUser(userDetails);
        School school = admin.getSchool();
        if (school == null) {
            throw new RuntimeException("School assignment not found for current admin");
        }
        campaign.setSchool(school);
        campaign.setCollectedAmount(BigDecimal.ZERO);
        if (batchId != null) {
            Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));
            if (batch.getSchool() == null || !school.getId().equals(batch.getSchool().getId())) {
                throw new RuntimeException("Unauthorized batch operation");
            }
            campaign.setBatch(batch);
        }
        fundCampaignService.create(campaign);
        logAction(
                admin,
                "SCHOOL_CAMPAIGN_CREATED",
                "FundCampaign",
                campaign.getId(),
                "Fund campaign created: " + campaign.getTitle()
        );
        redirect.addFlashAttribute("message", "ক্যাম্পেইন তৈরি হয়েছে");
        return "redirect:/school/fund-campaigns";
    }

    @PostMapping("/fund-campaign/{id}/close")
    public String closeCampaign(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        FundCampaign campaign = fundCampaignService.findById(id);
        if (campaign.getSchool() == null || !schoolId.equals(campaign.getSchool().getId())) {
            throw new RuntimeException("Unauthorized campaign operation");
        }
        fundCampaignService.closeCampaign(id);
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CAMPAIGN_CLOSED",
                "FundCampaign",
                id,
                "Fund campaign closed: " + campaign.getTitle()
        );
        redirect.addFlashAttribute("message", "ক্যাম্পেইন বন্ধ করা হয়েছে");
        return "redirect:/school/fund-campaigns";
    }

    @GetMapping("/fund-campaign/{id}/donations")
    public String viewDonations(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        Long schoolId = getSchoolId(userDetails);
        FundCampaign campaign = fundCampaignService.findById(id);
        if (campaign.getSchool() == null || !schoolId.equals(campaign.getSchool().getId())) {
            throw new RuntimeException("Unauthorized campaign operation");
        }
        List<Donation> donations = fundCampaignService.getDonations(id);
        model.addAttribute("campaign", campaign);
        model.addAttribute("donations", donations);
        return "school/donations";
    }

    @GetMapping("/gallery")
    public String gallery(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("items", galleryService.getApprovedBySchool(schoolId));
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/gallery";
    }

    @PostMapping("/gallery/upload")
    public String uploadGallery(@RequestParam("imageFile") MultipartFile imageFile,
                                @RequestParam(required = false) String caption,
                                @RequestParam(required = false) Long batchId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        User admin = getCurrentUser(userDetails);
        if (admin.getSchool() == null) {
            throw new RuntimeException("School assignment not found for current admin");
        }

        GalleryItem item = new GalleryItem();
        try {
            String saved = fileStorageService.storeImageFile(imageFile, "gallery");
            item.setUrl("/uploads/gallery/" + saved);
        } catch (Exception e) {
            redirect.addFlashAttribute("message", "ইমেজ আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/school/gallery";
        }
        item.setCaption(caption);
        item.setStatus(ApprovalStatus.APPROVED);
        item.setSchool(admin.getSchool());
        if (batchId != null) {
            Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));
            if (batch.getSchool() == null || !admin.getSchool().getId().equals(batch.getSchool().getId())) {
                throw new RuntimeException("Unauthorized batch operation");
            }
            item.setBatch(batch);
        }
        item.setUploadedBy(admin);
        galleryService.upload(item);
        logAction(admin, "SCHOOL_GALLERY_UPLOADED", "GalleryItem", item.getId(), "Gallery item uploaded");

        redirect.addFlashAttribute("message", "ছবি যোগ করা হয়েছে");
        return "redirect:/school/gallery";
    }

    @PostMapping("/gallery/delete/{id}")
    public String deleteGalleryItem(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        GalleryItem item = galleryItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Gallery item not found"));
        if (item.getSchool() == null || !schoolId.equals(item.getSchool().getId())) {
            throw new RuntimeException("Unauthorized gallery operation");
        }
        galleryService.delete(id);
        logAction(getCurrentUser(userDetails), "SCHOOL_GALLERY_DELETED", "GalleryItem", id, "Gallery item deleted");
        redirect.addFlashAttribute("message", "আইটেম মুছে ফেলা হয়েছে");
        return "redirect:/school/gallery";
    }

    @GetMapping("/profile")
    public String editSchoolProfile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("school", getCurrentSchool(userDetails));
        return "school/edit-profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute("school") School schoolForm,
                                @RequestParam(name = "logoFile", required = false) MultipartFile logoFile,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        School currentSchool = getCurrentSchool(userDetails);
        String logoUrl = currentSchool.getLogoUrl();
        try {
            if (logoFile != null && !logoFile.isEmpty()) {
                String saved = fileStorageService.storeImageFile(logoFile, "logos");
                logoUrl = "/uploads/logos/" + saved;
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("message", "লোগো আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/school/profile";
        }

        schoolService.updateSchoolProfile(
                schoolId,
                schoolForm.getName(),
                logoUrl,
                schoolForm.getAddress(),
                schoolForm.getPhone(),
                schoolForm.getEmail(),
                schoolForm.getWebsite(),
                schoolForm.getPrimaryColor(),
                schoolForm.getSecondaryColor()
        );
        redirect.addFlashAttribute("message", "প্রোফাইল আপডেট হয়েছে");
        return "redirect:/school/dashboard";
    }

    @PostMapping("/members/verify/{id}")
    public String verifyMember(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, id);
        memberService.verifyMember(id);
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_MEMBER_VERIFIED",
                "User",
                id,
                "Member verified: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্য ভেরিফাই করা হয়েছে");
        return "redirect:/school/members/pending";
    }

    @GetMapping("/members/list")
    public String membersList(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        return listAllMembers("", "ALL", "ALL", 0, 10, model, userDetails);
    }

    @GetMapping("/members")
    public String listAllMembers(@RequestParam(defaultValue = "") String q,
                                 @RequestParam(defaultValue = "ALL") String role,
                                 @RequestParam(defaultValue = "ALL") String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        List<Role> roles = Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER);
        List<User> allMembers = userRepository.findBySchoolIdAndEnabledTrueAndRoleIn(schoolId, roles);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        List<User> filtered = new ArrayList<>();
        for (User user : allMembers) {
            if (!"ALL".equalsIgnoreCase(role) && !user.getRole().name().equalsIgnoreCase(role)) {
                continue;
            }
            if ("SUSPENDED".equalsIgnoreCase(status) && !user.isSuspended()) {
                continue;
            }
            if ("ACTIVE".equalsIgnoreCase(status) && user.isSuspended()) {
                continue;
            }
            if ("UNVERIFIED".equalsIgnoreCase(status) && user.isVerified()) {
                continue;
            }
            if (!query.isEmpty()) {
                String text = (user.getFullName() + " " + user.getEmail() + " "
                        + (user.getBatch() == null ? "" : user.getBatch().getName()))
                        .toLowerCase(Locale.ROOT);
                if (!text.contains(query)) {
                    continue;
                }
            }
            filtered.add(user);
        }

        int pageSize = Math.max(5, Math.min(size, 50));
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int from = Math.min(currentPage * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);

        model.addAttribute("members", filtered.subList(from, to));
        model.addAttribute("q", q);
        model.addAttribute("role", role.toUpperCase(Locale.ROOT));
        model.addAttribute("status", status.toUpperCase(Locale.ROOT));
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        return "school/members";
    }

    @PostMapping("/members/make-cr/{memberId}")
    public String makeBatchController(@PathVariable Long memberId,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, memberId);
        memberService.assignBatchController(memberId);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনাকে ব্যাচ কন্ট্রোলার হিসেবে নিযুক্ত করা হয়েছে।",
                memberId
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_ASSIGNED",
                "User",
                memberId,
                "CR assigned from members page: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্যকে ব্যাচ কন্ট্রোলার করা হয়েছে");
        return "redirect:/school/members";
    }

    @PostMapping("/members/remove-cr/{memberId}")
    public String removeBatchController(@PathVariable Long memberId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, memberId);
        memberService.removeBatchController(memberId);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনাকে ব্যাচ কন্ট্রোলার ভূমিকা থেকে অপসারণ করা হয়েছে।",
                memberId
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_REMOVED",
                "User",
                memberId,
                "CR removed: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সিআর অপসারণ করা হয়েছে");
        return "redirect:/school/members";
    }

    @PostMapping("/members/suspend/{memberId}")
    public String suspendMember(@PathVariable Long memberId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, memberId);
        memberService.suspendMember(memberId);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনার অ্যাকাউন্ট সাময়িকভাবে স্থগিত (suspended) করা হয়েছে।",
                memberId
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_MEMBER_SUSPENDED",
                "User",
                memberId,
                "Member suspended: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্যকে suspend করা হয়েছে");
        return "redirect:/school/members";
    }

    @PostMapping("/members/unsuspend/{memberId}")
    public String unsuspendMember(@PathVariable Long memberId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User member = getManageableMember(schoolId, memberId);
        memberService.unsuspendMember(memberId);
        notificationService.create(
                member,
                NotificationType.APPROVAL,
                "আপনার অ্যাকাউন্ট পুনরায় চালু করা হয়েছে।",
                memberId
        );
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_MEMBER_UNSUSPENDED",
                "User",
                memberId,
                "Member unsuspended: " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্যের suspension তুলে দেওয়া হয়েছে");
        return "redirect:/school/members";
    }

    @PostMapping("/members/delete/{memberId}")
    public String deleteMember(@PathVariable Long memberId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        getManageableMember(schoolId, memberId);
        try {
            memberService.deleteMember(memberId);
            logAction(
                    getCurrentUser(userDetails),
                    "SCHOOL_MEMBER_DELETED",
                    "User",
                    memberId,
                    "Member deleted by school admin"
            );
            redirect.addFlashAttribute("message", "সদস্য মুছে ফেলা হয়েছে");
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute(
                    "message",
                    "সদস্য ডিলিট করা যায়নি (তার সাথে সম্পর্কিত ডেটা আছে)। আগে suspend করুন অথবা linked data সরান।"
            );
        }
        return "redirect:/school/members";
    }

    @GetMapping("/announcements")
    public String announcements(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("announcements", announcementRepository.findBySchoolIdAndBatchIsNullOrderByCreatedAtDesc(schoolId));
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/announcements";
    }

    @PostMapping("/announcements/create")
    public String createAnnouncement(@RequestParam String title,
                                     @RequestParam String content,
                                     @RequestParam(required = false) Long batchId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirect) {
        User admin = getCurrentUser(userDetails);
        if (admin.getSchool() == null) {
            throw new RuntimeException("School assignment not found for current admin");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setSchool(admin.getSchool());
        announcement.setCreatedBy(admin);

        if (batchId != null) {
            Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));
            if (batch.getSchool() == null || !admin.getSchool().getId().equals(batch.getSchool().getId())) {
                throw new RuntimeException("Unauthorized batch operation");
            }
            announcement.setBatch(batch);
        }
        announcementRepository.save(announcement);
        logAction(
                admin,
                "SCHOOL_ANNOUNCEMENT_CREATED",
                "Announcement",
                announcement.getId(),
                "Announcement created: " + title
        );

        if (announcement.getBatch() != null) {
            List<User> members = userRepository.findByBatchIdAndRole(announcement.getBatch().getId(), Role.MEMBER);
            List<User> controllers = userRepository.findByBatchIdAndRole(announcement.getBatch().getId(), Role.BATCH_CONTROLLER);
            for (User recipient : members) {
                notificationService.create(recipient, NotificationType.NOTICE, "নতুন ব্যাচ ঘোষণা: " + title, announcement.getId());
            }
            for (User recipient : controllers) {
                notificationService.create(recipient, NotificationType.NOTICE, "নতুন ব্যাচ ঘোষণা: " + title, announcement.getId());
            }
        } else {
            List<User> members = userRepository.findBySchoolIdAndRole(admin.getSchool().getId(), Role.MEMBER);
            List<User> controllers = userRepository.findBySchoolIdAndRole(admin.getSchool().getId(), Role.BATCH_CONTROLLER);
            for (User recipient : members) {
                notificationService.create(recipient, NotificationType.NOTICE, "নতুন স্কুল ঘোষণা: " + title, announcement.getId());
            }
            for (User recipient : controllers) {
                notificationService.create(recipient, NotificationType.NOTICE, "নতুন স্কুল ঘোষণা: " + title, announcement.getId());
            }
        }
        redirect.addFlashAttribute("message", "ঘোষণা পাঠানো হয়েছে");
        return "redirect:/school/announcements";
    }

    @GetMapping("/cr-permissions")
    public String crPermissions(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        BatchControllerPermissions permissions = getDefaultCrPermissions(schoolId);
        List<User> permissionTargets = userRepository.findBySchoolIdAndEnabledTrueAndRoleIn(
                schoolId,
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        );

        Map<Long, BatchControllerUserPermission> customPermissionMap = new HashMap<>();
        for (BatchControllerUserPermission custom : batchControllerUserPermissionRepository.findBySchoolId(schoolId)) {
            if (custom.getUser() != null) {
                customPermissionMap.put(custom.getUser().getId(), custom);
            }
        }

        model.addAttribute("permissions", permissions);
        model.addAttribute("permissionTargets", permissionTargets);
        model.addAttribute("customPermissionMap", customPermissionMap);
        return "school/cr-permissions";
    }

    @PostMapping("/cr-permissions")
    public String saveCRPermissions(@ModelAttribute("permissions") BatchControllerPermissions permissions,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        permissions.setSchoolId(schoolId);
        batchControllerPermissionsRepository.save(permissions);
        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_PERMISSIONS_UPDATED",
                "BatchControllerPermissions",
                schoolId,
                "Default CR permissions updated"
        );
        redirect.addFlashAttribute("message", "ডিফল্ট সিআর পারমিশন আপডেট হয়েছে");
        return "redirect:/school/cr-permissions";
    }

    @PostMapping("/cr-permissions/user/{userId}")
    public String saveCRUserPermissions(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "false") boolean canApprovePosts,
                                        @RequestParam(defaultValue = "false") boolean canCreateNotices,
                                        @RequestParam(defaultValue = "false") boolean canUploadGallery,
                                        @RequestParam(defaultValue = "false") boolean canModerateMembers,
                                        @AuthenticationPrincipal UserDetails userDetails,
                                        RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User target = getPermissionTargetMember(schoolId, userId);

        BatchControllerUserPermission custom = batchControllerUserPermissionRepository
                .findBySchoolIdAndUserId(schoolId, userId)
                .orElseGet(BatchControllerUserPermission::new);
        custom.setSchoolId(schoolId);
        custom.setUser(target);
        custom.setCanApprovePosts(canApprovePosts);
        custom.setCanCreateNotices(canCreateNotices);
        custom.setCanUploadGallery(canUploadGallery);
        custom.setCanModerateMembers(canModerateMembers);
        batchControllerUserPermissionRepository.save(custom);

        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_USER_PERMISSIONS_UPDATED",
                "BatchControllerUserPermission",
                userId,
                "Custom CR permissions updated for " + target.getEmail()
        );
        redirect.addFlashAttribute("message", "ব্যক্তিগত পারমিশন আপডেট হয়েছে: " + target.getFullName());
        return "redirect:/school/cr-permissions";
    }

    @PostMapping("/cr-permissions/user/{userId}/reset")
    public String resetCRUserPermissions(@PathVariable Long userId,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        User target = getPermissionTargetMember(schoolId, userId);
        batchControllerUserPermissionRepository.deleteBySchoolIdAndUserId(schoolId, userId);

        logAction(
                getCurrentUser(userDetails),
                "SCHOOL_CR_USER_PERMISSIONS_RESET",
                "BatchControllerUserPermission",
                userId,
                "Custom CR permissions reset for " + target.getEmail()
        );
        redirect.addFlashAttribute("message", "ব্যক্তিগত পারমিশন রিসেট করা হয়েছে: " + target.getFullName());
        return "redirect:/school/cr-permissions";
    }

    @GetMapping("/analytics")
    public String schoolAnalytics(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        long members = userRepository.countBySchoolIdAndRole(schoolId, Role.MEMBER);
        long controllers = userRepository.countBySchoolIdAndRole(schoolId, Role.BATCH_CONTROLLER);
        long pendingMembers = userRepository.countBySchoolIdAndEnabledFalseAndRole(schoolId, Role.MEMBER);
        long batches = batchRepository.countBySchoolId(schoolId);
        long activeFunds = fundCampaignRepository.countBySchoolIdAndClosedFalse(schoolId);
        long pendingPosts = postRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.PENDING).size();

        model.addAttribute("totalMembers", members + controllers);
        model.addAttribute("pendingMembers", pendingMembers);
        model.addAttribute("totalBatches", batches);
        model.addAttribute("activeFunds", activeFunds);
        model.addAttribute("pendingPosts", pendingPosts);
        return "school/analytics";
    }

    @GetMapping("/bulk-import")
    public String bulkImportForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Long schoolId = getSchoolId(userDetails);
        model.addAttribute("batches", batchRepository.findBySchoolId(schoolId));
        return "school/bulk-import";
    }

    @PostMapping("/bulk-import")
    public String handleBulkImport(@RequestParam MultipartFile file,
                                   @RequestParam Long batchId,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirect) {
        Long schoolId = getSchoolId(userDetails);
        Batch batch = batchRepository.findById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));
        if (batch.getSchool() == null || !schoolId.equals(batch.getSchool().getId())) {
            throw new RuntimeException("Unauthorized batch operation");
        }
        try {
            int imported = bulkImportService.importAlumni(file, batchId);
            logAction(
                    getCurrentUser(userDetails),
                    "SCHOOL_BULK_IMPORT_COMPLETED",
                    "Batch",
                    batchId,
                    "Bulk import completed. Imported users: " + imported
            );
            redirect.addFlashAttribute("message", "সদস্য ইম্পোর্ট সম্পন্ন হয়েছে: " + imported);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "ইম্পোর্ট ব্যর্থ: " + e.getMessage());
        }
        return "redirect:/school/bulk-import";
    }
}
