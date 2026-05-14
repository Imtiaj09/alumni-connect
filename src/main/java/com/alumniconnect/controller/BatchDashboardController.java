package com.alumniconnect.controller;

import com.alumniconnect.entity.ActivityLog;
import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.Batch;
import com.alumniconnect.entity.BatchControllerPermissions;
import com.alumniconnect.entity.BatchControllerUserPermission;
import com.alumniconnect.entity.GalleryItem;
import com.alumniconnect.entity.Notice;
import com.alumniconnect.entity.NotificationType;
import com.alumniconnect.entity.Post;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.BatchControllerPermissionsRepository;
import com.alumniconnect.repository.BatchControllerUserPermissionRepository;
import com.alumniconnect.repository.GalleryItemRepository;
import com.alumniconnect.repository.PostRepository;
import com.alumniconnect.repository.UserRepository;
import com.alumniconnect.service.ActivityLogService;
import com.alumniconnect.service.BatchService;
import com.alumniconnect.service.FileStorageService;
import com.alumniconnect.service.GalleryService;
import com.alumniconnect.service.MemberService;
import com.alumniconnect.service.NoticeService;
import com.alumniconnect.service.NotificationService;
import com.alumniconnect.service.PostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/batch")
@PreAuthorize("hasRole('BATCH_CONTROLLER')")
public class BatchDashboardController {

    private final UserRepository userRepository;
    private final NoticeService noticeService;
    private final PostRepository postRepository;
    private final PostService postService;
    private final BatchService batchService;
    private final BatchControllerPermissionsRepository batchControllerPermissionsRepository;
    private final BatchControllerUserPermissionRepository batchControllerUserPermissionRepository;
    private final GalleryService galleryService;
    private final GalleryItemRepository galleryItemRepository;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;

    public BatchDashboardController(UserRepository userRepository,
                                    NoticeService noticeService,
                                    PostRepository postRepository,
                                    PostService postService,
                                    BatchService batchService,
                                    BatchControllerPermissionsRepository batchControllerPermissionsRepository,
                                    BatchControllerUserPermissionRepository batchControllerUserPermissionRepository,
                                    GalleryService galleryService,
                                    GalleryItemRepository galleryItemRepository,
                                    MemberService memberService,
                                    NotificationService notificationService,
                                    ActivityLogService activityLogService,
                                    FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.noticeService = noticeService;
        this.postRepository = postRepository;
        this.postService = postService;
        this.batchService = batchService;
        this.batchControllerPermissionsRepository = batchControllerPermissionsRepository;
        this.batchControllerUserPermissionRepository = batchControllerUserPermissionRepository;
        this.galleryService = galleryService;
        this.galleryItemRepository = galleryItemRepository;
        this.memberService = memberService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.fileStorageService = fileStorageService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Batch getCurrentBatch(UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getBatch() == null) {
            throw new RuntimeException("Batch assignment not found for current user");
        }
        return user.getBatch();
    }

    private BatchControllerPermissions getDefaultPermissions(Batch batch) {
        if (batch.getSchool() == null) {
            return new BatchControllerPermissions();
        }
        return batchControllerPermissionsRepository.findById(batch.getSchool().getId())
                .orElseGet(() -> {
                    BatchControllerPermissions permissions = new BatchControllerPermissions();
                    permissions.setSchoolId(batch.getSchool().getId());
                    return permissions;
                });
    }

    private BatchControllerPermissions mergeUserOverride(BatchControllerPermissions defaults,
                                                         BatchControllerUserPermission custom) {
        BatchControllerPermissions effective = new BatchControllerPermissions();
        effective.setSchoolId(defaults.getSchoolId());
        effective.setCanApprovePosts(custom.isCanApprovePosts());
        effective.setCanCreateNotices(custom.isCanCreateNotices());
        effective.setCanUploadGallery(custom.isCanUploadGallery());
        effective.setCanModerateMembers(custom.isCanModerateMembers());
        return effective;
    }

    private BatchControllerPermissions getPermissions(Batch batch) {
        BatchControllerPermissions defaults = getDefaultPermissions(batch);
        if (batch.getSchool() == null) {
            return defaults;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            return defaults;
        }

        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return defaults;
        }

        return batchControllerUserPermissionRepository
                .findBySchoolIdAndUserId(batch.getSchool().getId(), currentUser.getId())
                .map(custom -> mergeUserOverride(defaults, custom))
                .orElse(defaults);
    }

    private void requirePermission(boolean allowed, String message) {
        if (!allowed) {
            throw new RuntimeException(message);
        }
    }

    private User getBatchMemberOrThrow(Long batchId, Long memberId, boolean allowPending) {
        User member = userRepository.findById(memberId).orElseThrow(() -> new RuntimeException("Member not found"));
        if (member.getBatch() == null || !batchId.equals(member.getBatch().getId())) {
            throw new RuntimeException("Unauthorized member operation");
        }
        if (member.getRole() != Role.MEMBER && member.getRole() != Role.BATCH_CONTROLLER) {
            throw new RuntimeException("This account is not manageable as batch member");
        }
        if (!allowPending && !member.isEnabled()) {
            throw new RuntimeException("Pending সদস্যের উপর এই অ্যাকশন করা যাবে না");
        }
        return member;
    }

    private void logAction(User actor, String action, String entityType, Long entityId, String description) {
        activityLogService.log(new ActivityLog(action, entityType, entityId, description, actor));
    }

    @ModelAttribute("crPermissions")
    public BatchControllerPermissions exposeCrPermissions(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return new BatchControllerPermissions();
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || user.getBatch() == null) {
            return new BatchControllerPermissions();
        }
        return getPermissions(user.getBatch());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Batch batch = getCurrentBatch(userDetails);
        long memberCount = batchService.getMembersByBatch(batch.getId()).size();
        long noticeCount = noticeService.getNoticesByBatch(batch.getId()).size();
        long pendingPosts = postRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.PENDING).size();
        long pendingMembers = userRepository.findByBatchIdAndEnabledFalseAndRoleIn(
                batch.getId(),
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        ).size();
        long galleryItems = galleryItemRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.APPROVED).size();

        model.addAttribute("batch", batch);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("noticeCount", noticeCount);
        model.addAttribute("pendingPosts", pendingPosts);
        model.addAttribute("pendingMembers", pendingMembers);
        model.addAttribute("galleryItems", galleryItems);
        return "batch/dashboard";
    }

    @GetMapping("/notices")
    public String notices(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Batch batch = getCurrentBatch(userDetails);
        model.addAttribute("notices", noticeService.getNoticesByBatch(batch.getId()));
        model.addAttribute("batch", batch);
        return "batch/notices";
    }

    @PostMapping("/notices/create")
    public String createNotice(@RequestParam String title,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "false") boolean pinned,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanCreateNotices(), "স্কুল অ্যাডমিন নোটিশ তৈরির অনুমতি দেননি");

        User currentUser = getCurrentUser(userDetails);
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPinned(pinned);
        notice.setBatch(batch);
        notice.setCreatedBy(currentUser);
        noticeService.save(notice);

        logAction(
                currentUser,
                "BATCH_NOTICE_CREATED",
                "Notice",
                notice.getId(),
                "Batch notice created in " + batch.getName() + ": " + title
        );
        redirect.addFlashAttribute("message", "নোটিশ তৈরি হয়েছে");
        return "redirect:/batch/notices";
    }

    @PostMapping("/notices/delete/{id}")
    public String deleteNotice(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanCreateNotices(), "স্কুল অ্যাডমিন নোটিশ মুছার অনুমতি দেননি");

        Notice notice = noticeService.getNoticesByBatch(batch.getId()).stream()
                .filter(n -> id.equals(n.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notice not found in your batch"));
        noticeService.delete(notice.getId());

        logAction(
                getCurrentUser(userDetails),
                "BATCH_NOTICE_DELETED",
                "Notice",
                notice.getId(),
                "Batch notice deleted from " + batch.getName() + ": " + notice.getTitle()
        );
        redirect.addFlashAttribute("message", "নোটিশ মুছে ফেলা হয়েছে");
        return "redirect:/batch/notices";
    }

    @GetMapping("/posts/pending")
    public String pendingPosts(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Batch batch = getCurrentBatch(userDetails);
        List<Post> pending = postRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.PENDING);
        model.addAttribute("pendingPosts", pending);
        model.addAttribute("batch", batch);
        return "batch/posts-moderation";
    }

    @PostMapping("/posts/approve/{id}")
    public String approvePost(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanApprovePosts(), "স্কুল অ্যাডমিন পোস্ট অনুমোদনের অনুমতি দেননি");

        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
        if (post.getBatch() == null || !batch.getId().equals(post.getBatch().getId())) {
            throw new RuntimeException("Unauthorized post operation");
        }
        postService.approvePost(id);
        if (post.getCreatedBy() != null) {
            notificationService.create(post.getCreatedBy(), NotificationType.POST_APPROVED, "আপনার পোস্ট অনুমোদিত হয়েছে।", id);
        }
        logAction(
                getCurrentUser(userDetails),
                "BATCH_POST_APPROVED",
                "Post",
                id,
                "Batch post approved in " + batch.getName() + ": " + post.getTitle()
        );
        redirect.addFlashAttribute("message", "পোস্ট অনুমোদিত হয়েছে");
        return "redirect:/batch/posts/pending";
    }

    @PostMapping("/posts/reject/{id}")
    public String rejectPost(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanApprovePosts(), "স্কুল অ্যাডমিন পোস্ট অনুমোদনের অনুমতি দেননি");

        Post post = postRepository.findById(id).orElseThrow(() -> new RuntimeException("Post not found"));
        if (post.getBatch() == null || !batch.getId().equals(post.getBatch().getId())) {
            throw new RuntimeException("Unauthorized post operation");
        }
        postService.rejectPost(id);
        if (post.getCreatedBy() != null) {
            notificationService.create(post.getCreatedBy(), NotificationType.POST_REJECTED, "আপনার পোস্ট প্রত্যাখ্যান করা হয়েছে।", id);
        }
        logAction(
                getCurrentUser(userDetails),
                "BATCH_POST_REJECTED",
                "Post",
                id,
                "Batch post rejected in " + batch.getName() + ": " + post.getTitle()
        );
        redirect.addFlashAttribute("message", "পোস্ট প্রত্যাখ্যান হয়েছে");
        return "redirect:/batch/posts/pending";
    }

    @GetMapping("/gallery")
    public String gallery(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Batch batch = getCurrentBatch(userDetails);
        model.addAttribute("batch", batch);
        model.addAttribute("items", galleryItemRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.APPROVED));
        return "batch/gallery";
    }

    @PostMapping("/gallery/upload")
    public String uploadGallery(@RequestParam("imageFile") MultipartFile imageFile,
                                @RequestParam(required = false) String caption,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanUploadGallery(), "স্কুল অ্যাডমিন গ্যালারি আপলোডের অনুমতি দেননি");

        User currentUser = getCurrentUser(userDetails);
        GalleryItem item = new GalleryItem();
        try {
            String saved = fileStorageService.storeImageFile(imageFile, "gallery");
            item.setUrl("/uploads/gallery/" + saved);
        } catch (Exception e) {
            redirect.addFlashAttribute("message", "ইমেজ আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/batch/gallery";
        }
        item.setCaption(caption);
        item.setBatch(batch);
        item.setSchool(batch.getSchool());
        item.setStatus(ApprovalStatus.PENDING);
        item.setUploadedBy(currentUser);
        galleryService.upload(item);

        logAction(
                currentUser,
                "BATCH_GALLERY_UPLOADED",
                "GalleryItem",
                item.getId(),
                "Gallery item uploaded to batch " + batch.getName()
        );
        redirect.addFlashAttribute("message", "গ্যালারি আইটেম জমা হয়েছে, স্কুল অ্যাডমিন অনুমোদনের পর প্রকাশ পাবে");
        return "redirect:/batch/gallery";
    }

    @PostMapping("/gallery/delete/{id}")
    public String deleteGallery(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanUploadGallery(), "স্কুল অ্যাডমিন গ্যালারি মুছার অনুমতি দেননি");

        GalleryItem item = galleryItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Gallery item not found"));
        if (item.getBatch() == null || !batch.getId().equals(item.getBatch().getId())) {
            throw new RuntimeException("Unauthorized gallery operation");
        }
        galleryService.delete(id);
        logAction(
                getCurrentUser(userDetails),
                "BATCH_GALLERY_DELETED",
                "GalleryItem",
                id,
                "Gallery item deleted from batch " + batch.getName()
        );
        redirect.addFlashAttribute("message", "গ্যালারি আইটেম মুছে ফেলা হয়েছে");
        return "redirect:/batch/gallery";
    }

    @GetMapping("/members")
    public String members(@RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "ALL") String role,
                          @RequestParam(defaultValue = "ALL") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          Model model,
                          @AuthenticationPrincipal UserDetails userDetails) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanModerateMembers(), "স্কুল অ্যাডমিন সদস্য মডারেশনের অনুমতি দেননি");

        List<User> activeMembers = userRepository.findByBatchIdAndRoleIn(
                batch.getId(),
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        );
        List<User> pendingMembers = userRepository.findByBatchIdAndEnabledFalseAndRoleIn(
                batch.getId(),
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        );

        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<User> filtered = new ArrayList<>();
        for (User user : activeMembers) {
            if (!user.isEnabled()) {
                continue;
            }
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
                String text = (user.getFullName() + " " + user.getEmail() + " " + (user.getRollNumber() == null ? "" : user.getRollNumber()))
                        .toLowerCase(Locale.ROOT);
                if (!text.contains(query)) {
                    continue;
                }
            }
            filtered.add(user);
        }

        int pageSize = Math.max(5, Math.min(size, 50));
        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int from = Math.min(currentPage * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<User> pageItems = filtered.subList(from, to);

        model.addAttribute("batch", batch);
        model.addAttribute("members", pageItems);
        model.addAttribute("pendingMembers", pendingMembers);
        model.addAttribute("q", q);
        model.addAttribute("role", role.toUpperCase(Locale.ROOT));
        model.addAttribute("status", status.toUpperCase(Locale.ROOT));
        model.addAttribute("page", currentPage);
        model.addAttribute("size", pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", total);
        return "batch/members";
    }

    @PostMapping("/members/approve/{id}")
    public String approveMember(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanModerateMembers(), "স্কুল অ্যাডমিন সদস্য অনুমোদনের অনুমতি দেননি");

        User member = getBatchMemberOrThrow(batch.getId(), id, true);
        memberService.approveMember(id);
        notificationService.create(member, NotificationType.APPROVAL, "আপনার সদস্যপদ অনুমোদিত হয়েছে।", id);
        logAction(
                getCurrentUser(userDetails),
                "BATCH_MEMBER_APPROVED",
                "User",
                id,
                "Member approved in batch " + batch.getName() + ": " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্য অনুমোদিত হয়েছে");
        return "redirect:/batch/members";
    }

    @PostMapping("/members/reject/{id}")
    public String rejectMember(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanModerateMembers(), "স্কুল অ্যাডমিন সদস্য বাতিলের অনুমতি দেননি");

        User member = getBatchMemberOrThrow(batch.getId(), id, true);
        memberService.rejectMember(id);
        logAction(
                getCurrentUser(userDetails),
                "BATCH_MEMBER_REJECTED",
                "User",
                id,
                "Member request rejected in batch " + batch.getName() + ": " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্যের রিকোয়েস্ট বাতিল করা হয়েছে");
        return "redirect:/batch/members";
    }

    @PostMapping("/members/verify/{id}")
    public String verifyMember(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirect) {
        Batch batch = getCurrentBatch(userDetails);
        BatchControllerPermissions permissions = getPermissions(batch);
        requirePermission(permissions.isCanModerateMembers(), "স্কুল অ্যাডমিন সদস্য ভেরিফিকেশনের অনুমতি দেননি");

        User member = getBatchMemberOrThrow(batch.getId(), id, false);
        memberService.verifyMember(id);
        notificationService.create(member, NotificationType.APPROVAL, "আপনার অ্যালামনাই প্রোফাইল ভেরিফাই করা হয়েছে।", id);
        logAction(
                getCurrentUser(userDetails),
                "BATCH_MEMBER_VERIFIED",
                "User",
                id,
                "Member verified in batch " + batch.getName() + ": " + member.getEmail()
        );
        redirect.addFlashAttribute("message", "সদস্য ভেরিফাই করা হয়েছে");
        return "redirect:/batch/members";
    }
}
