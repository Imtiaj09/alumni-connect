package com.alumniconnect.controller;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.Announcement;
import com.alumniconnect.entity.Batch;
import com.alumniconnect.entity.Donation;
import com.alumniconnect.entity.FundCampaign;
import com.alumniconnect.entity.GalleryItem;
import com.alumniconnect.entity.Notification;
import com.alumniconnect.entity.Post;
import com.alumniconnect.entity.PostType;
import com.alumniconnect.entity.RSVP;
import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.AnnouncementRepository;
import com.alumniconnect.repository.DonationRepository;
import com.alumniconnect.repository.FundCampaignRepository;
import com.alumniconnect.repository.GalleryItemRepository;
import com.alumniconnect.repository.NoticeRepository;
import com.alumniconnect.repository.PostRepository;
import com.alumniconnect.repository.RSVPRepository;
import com.alumniconnect.repository.UserRepository;
import com.alumniconnect.service.FundCampaignService;
import com.alumniconnect.service.FileStorageService;
import com.alumniconnect.service.GalleryService;
import com.alumniconnect.service.MessageService;
import com.alumniconnect.service.MemberService;
import com.alumniconnect.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/member")
@PreAuthorize("hasAnyRole('MEMBER','BATCH_CONTROLLER')")
public class MemberController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final NoticeRepository noticeRepository;
    private final FundCampaignRepository fundCampaignRepository;
    private final GalleryItemRepository galleryItemRepository;
    private final AnnouncementRepository announcementRepository;
    private final RSVPRepository rsvpRepository;
    private final DonationRepository donationRepository;
    private final FundCampaignService fundCampaignService;
    private final FileStorageService fileStorageService;
    private final GalleryService galleryService;
    private final MessageService messageService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public MemberController(UserRepository userRepository,
                            PostRepository postRepository,
                            NoticeRepository noticeRepository,
                            FundCampaignRepository fundCampaignRepository,
                            GalleryItemRepository galleryItemRepository,
                            AnnouncementRepository announcementRepository,
                            RSVPRepository rsvpRepository,
                            DonationRepository donationRepository,
                            FundCampaignService fundCampaignService,
                            FileStorageService fileStorageService,
                            GalleryService galleryService,
                            MessageService messageService,
                            MemberService memberService,
                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.noticeRepository = noticeRepository;
        this.fundCampaignRepository = fundCampaignRepository;
        this.galleryItemRepository = galleryItemRepository;
        this.announcementRepository = announcementRepository;
        this.rsvpRepository = rsvpRepository;
        this.donationRepository = donationRepository;
        this.fundCampaignService = fundCampaignService;
        this.fileStorageService = fileStorageService;
        this.galleryService = galleryService;
        this.messageService = messageService;
        this.memberService = memberService;
        this.notificationService = notificationService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void ensureMemberHasSchoolAndBatch(User user) {
        if (user.getSchool() == null || user.getBatch() == null) {
            throw new RuntimeException("Member context is incomplete");
        }
    }

    private void ensureCampaignAccessible(User user, FundCampaign campaign) {
        if (campaign.getSchool() == null || user.getSchool() == null
                || !campaign.getSchool().getId().equals(user.getSchool().getId())) {
            throw new RuntimeException("Unauthorized campaign access");
        }
        if (campaign.getBatch() != null) {
            if (user.getBatch() == null || !campaign.getBatch().getId().equals(user.getBatch().getId())) {
                throw new RuntimeException("Unauthorized batch campaign access");
            }
        }
    }

    private void ensureEventAccessible(User user, Post post) {
        if (post.getSchool() == null || user.getSchool() == null
                || !post.getSchool().getId().equals(user.getSchool().getId())) {
            throw new RuntimeException("Unauthorized event access");
        }
        if (post.getBatch() != null) {
            if (user.getBatch() == null || !post.getBatch().getId().equals(user.getBatch().getId())) {
                throw new RuntimeException("Unauthorized batch event access");
            }
        }
    }

    private int getNextProfileMilestone(int profileCompletion) {
        if (profileCompletion < 25) {
            return 25;
        }
        if (profileCompletion < 50) {
            return 50;
        }
        if (profileCompletion < 75) {
            return 75;
        }
        return 100;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Batch batch = user.getBatch();
        if (batch == null) {
            throw new RuntimeException("Batch assignment not found for current member");
        }

        List<com.alumniconnect.entity.Notice> recentNotices =
                noticeRepository.findByBatchIdOrderByPinnedDescCreatedAtDesc(batch.getId());
        if (recentNotices.size() > 5) {
            recentNotices = recentNotices.subList(0, 5);
        }

        List<Post> approvedPosts = postRepository.findByBatchIdAndStatus(batch.getId(), ApprovalStatus.APPROVED);
        LocalDateTime now = LocalDateTime.now();
        List<Post> upcomingBatchEvents = postRepository.findByBatchIdAndTypeAndStatusAndEventDateGreaterThanEqual(
                batch.getId(), PostType.EVENT, ApprovalStatus.APPROVED, now
        );
        List<Post> upcomingSchoolEvents = postRepository.findBySchoolIdAndBatchIsNullAndTypeAndStatusAndEventDateGreaterThanEqual(
                user.getSchool().getId(), PostType.EVENT, ApprovalStatus.APPROVED, now
        );
        int upcomingEventsCount = upcomingBatchEvents.size() + upcomingSchoolEvents.size();
        int pendingRsvpCount = 0;
        for (Post post : upcomingBatchEvents) {
            if (rsvpRepository.findByPostIdAndUserId(post.getId(), user.getId()).isEmpty()) {
                pendingRsvpCount++;
            }
        }
        for (Post post : upcomingSchoolEvents) {
            if (rsvpRepository.findByPostIdAndUserId(post.getId(), user.getId()).isEmpty()) {
                pendingRsvpCount++;
            }
        }
        long activeBatchCampaignCount = fundCampaignRepository.findByBatchIdAndClosedFalse(batch.getId()).size();
        long unreadMessages = messageService.getUnreadCount(user.getId());
        long unreadNotifications = notificationService.unreadCount(user.getId());
        long pendingPostApprovals = postRepository.countByCreatedByIdAndStatus(user.getId(), ApprovalStatus.PENDING);
        long pendingGalleryApprovals = galleryItemRepository.countByUploadedByIdAndStatus(user.getId(), ApprovalStatus.PENDING);
        int profileGap = Math.max(0, 100 - user.getProfileCompletion());
        int nextProfileMilestone = getNextProfileMilestone(user.getProfileCompletion());
        int dashboardAttentionCount = (int) Math.min(
                99L,
                unreadMessages + unreadNotifications + pendingRsvpCount + pendingPostApprovals + pendingGalleryApprovals
        );
        long memberCount = userRepository.findByBatchIdAndRoleIn(
                batch.getId(),
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        ).size();
        List<Post> upcomingEventsPreview = new ArrayList<>();
        upcomingEventsPreview.addAll(upcomingBatchEvents);
        upcomingEventsPreview.addAll(upcomingSchoolEvents);
        upcomingEventsPreview.sort(Comparator.comparing(Post::getEventDate));
        if (upcomingEventsPreview.size() > 4) {
            upcomingEventsPreview = upcomingEventsPreview.subList(0, 4);
        }

        List<Post> approvedPostsPreview = approvedPosts.stream()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .limit(4)
                .toList();
        List<User> spotlightMembers = userRepository.findByBatchIdAndRoleIn(
                        batch.getId(),
                        Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
                ).stream()
                .filter(User::isEnabled)
                .filter(member -> !member.isSuspended())
                .filter(member -> !member.getId().equals(user.getId()))
                .limit(5)
                .toList();
        BigDecimal myDonationTotal = donationRepository.getTotalDonationsByDonorId(user.getId());
        long myDonationCount = donationRepository.findByDonorId(user.getId()).size();

        model.addAttribute("user", user);
        model.addAttribute("batch", batch);
        model.addAttribute("recentNotices", recentNotices);
        model.addAttribute("approvedPosts", approvedPostsPreview);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("profileGap", profileGap);
        model.addAttribute("nextProfileMilestone", nextProfileMilestone);
        model.addAttribute("dashboardAttentionCount", dashboardAttentionCount);
        model.addAttribute("unreadMessages", unreadMessages);
        model.addAttribute("unreadNotifications", unreadNotifications);
        model.addAttribute("upcomingEventsCount", upcomingEventsCount);
        model.addAttribute("upcomingEventsPreview", upcomingEventsPreview);
        model.addAttribute("pendingRsvpCount", pendingRsvpCount);
        model.addAttribute("pendingPostApprovals", pendingPostApprovals);
        model.addAttribute("pendingGalleryApprovals", pendingGalleryApprovals);
        model.addAttribute("activeBatchCampaignCount", activeBatchCampaignCount);
        model.addAttribute("spotlightMembers", spotlightMembers);
        model.addAttribute("myDonationTotal", myDonationTotal);
        model.addAttribute("myDonationCount", myDonationCount);
        return "member/dashboard";
    }

    @GetMapping("/directory")
    public String directory(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        Batch batch = currentUser.getBatch();
        if (batch == null) {
            throw new RuntimeException("Batch assignment not found for current member");
        }

        List<User> members = new ArrayList<>(userRepository.findByBatchIdAndRoleIn(
                batch.getId(),
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        ));
        model.addAttribute("members", members);
        model.addAttribute("batch", batch);
        return "member/directory";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("profileCompletion", user.getProfileCompletion());
        return "member/profile-edit";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(@ModelAttribute("user") User updatedUser,
                                @RequestParam(name = "profilePhotoFile", required = false) MultipartFile profilePhotoFile,
                                @RequestParam(name = "coverPhotoFile", required = false) MultipartFile coverPhotoFile,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        User currentUser = getCurrentUser(userDetails);
        try {
            if (profilePhotoFile != null && !profilePhotoFile.isEmpty()) {
                String saved = fileStorageService.storeFile(profilePhotoFile, "profiles");
                updatedUser.setProfilePhotoUrl("/uploads/profiles/" + saved);
            } else {
                updatedUser.setProfilePhotoUrl(currentUser.getProfilePhotoUrl());
            }

            if (coverPhotoFile != null && !coverPhotoFile.isEmpty()) {
                String saved = fileStorageService.storeFile(coverPhotoFile, "covers");
                updatedUser.setCoverPhotoUrl("/uploads/covers/" + saved);
            } else {
                updatedUser.setCoverPhotoUrl(currentUser.getCoverPhotoUrl());
            }
        } catch (IOException | IllegalArgumentException e) {
            redirect.addFlashAttribute("message", "ফাইল আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/member/profile/edit";
        }

        memberService.editProfile(currentUser, updatedUser);
        redirect.addFlashAttribute("message", "প্রোফাইল আপডেট হয়েছে");
        return "redirect:/member/dashboard";
    }

    @GetMapping("/post/create")
    public String createPostForm(Model model) {
        model.addAttribute("post", new Post());
        return "member/create-post";
    }

    @PostMapping("/post/create")
    public String createPost(@ModelAttribute("post") Post post,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirect) {
        User user = getCurrentUser(userDetails);
        post.setCreatedBy(user);
        post.setBatch(user.getBatch());
        post.setSchool(user.getSchool());
        post.setStatus(ApprovalStatus.PENDING);
        post.setType(post.getType() == null ? PostType.GENERAL : post.getType());
        postRepository.save(post);
        redirect.addFlashAttribute("message", "পোস্ট জমা দেওয়া হয়েছে, অনুমোদনের পর প্রকাশিত হবে");
        return "redirect:/member/dashboard";
    }

    @GetMapping("/fund-campaigns")
    public String listCampaigns(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        ensureMemberHasSchoolAndBatch(user);
        List<FundCampaign> schoolCampaigns = fundCampaignRepository.findBySchoolIdAndClosedFalse(user.getSchool().getId());
        List<FundCampaign> batchCampaigns = user.getBatch() == null
                ? List.of()
                : fundCampaignRepository.findByBatchIdAndClosedFalse(user.getBatch().getId());
        model.addAttribute("schoolCampaigns", schoolCampaigns);
        model.addAttribute("batchCampaigns", batchCampaigns);
        return "member/fund-campaigns";
    }

    @GetMapping("/fund-campaign/{id}/donate")
    public String donateForm(@PathVariable Long id,
                             Model model,
                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        ensureMemberHasSchoolAndBatch(user);
        FundCampaign campaign = fundCampaignService.findById(id);
        ensureCampaignAccessible(user, campaign);
        if (campaign.isClosed()) {
            throw new RuntimeException("Campaign is closed");
        }
        model.addAttribute("campaign", campaign);
        model.addAttribute("donation", new Donation());
        return "member/donate";
    }

    @PostMapping("/fund-campaign/{id}/donate")
    public String donate(@PathVariable Long id,
                         @ModelAttribute("donation") Donation donation,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirect) {
        User donor = getCurrentUser(userDetails);
        ensureMemberHasSchoolAndBatch(donor);
        FundCampaign campaign = fundCampaignService.findById(id);
        ensureCampaignAccessible(donor, campaign);
        if (campaign.isClosed()) {
            redirect.addFlashAttribute("message", "ক্যাম্পেইনটি বন্ধ রয়েছে");
            return "redirect:/member/fund-campaigns";
        }
        donation.setDonor(donor);
        fundCampaignService.donate(donation, campaign);
        redirect.addFlashAttribute("message", "অনুদান সফল হয়েছে");
        return "redirect:/member/fund-campaigns";
    }

    @GetMapping("/gallery")
    public String viewGallery(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        ensureMemberHasSchoolAndBatch(user);
        List<GalleryItem> schoolItems = galleryService.getApprovedBySchool(user.getSchool().getId());
        List<GalleryItem> batchItems = user.getBatch() == null
                ? List.of()
                : galleryService.getApprovedByBatch(user.getBatch().getId());
        model.addAttribute("schoolItems", schoolItems);
        model.addAttribute("batchItems", batchItems);
        return "member/gallery";
    }

    @PostMapping("/gallery/upload")
    public String uploadGallery(@RequestParam("imageFile") MultipartFile imageFile,
                                @RequestParam(required = false) String caption,
                                @RequestParam(defaultValue = "BATCH") String scope,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirect) {
        User user = getCurrentUser(userDetails);
        if (user.getSchool() == null) {
            throw new RuntimeException("School assignment not found for current member");
        }

        GalleryItem item = new GalleryItem();
        try {
            String saved = fileStorageService.storeImageFile(imageFile, "gallery");
            item.setUrl("/uploads/gallery/" + saved);
        } catch (IOException | IllegalArgumentException e) {
            redirect.addFlashAttribute("message", "ইমেজ আপলোড ব্যর্থ: " + e.getMessage());
            return "redirect:/member/gallery";
        }

        item.setCaption(caption);
        item.setSchool(user.getSchool());
        item.setUploadedBy(user);
        item.setStatus(ApprovalStatus.PENDING);

        if ("BATCH".equalsIgnoreCase(scope) && user.getBatch() != null) {
            item.setBatch(user.getBatch());
        }

        galleryService.upload(item);
        redirect.addFlashAttribute("message", "গ্যালারি আইটেম জমা হয়েছে, অনুমোদনের পর দেখা যাবে");
        return "redirect:/member/gallery";
    }

    @GetMapping("/events")
    public String events(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getBatch() == null || user.getSchool() == null) {
            throw new RuntimeException("Member context is incomplete");
        }

        List<Post> batchEvents = postRepository.findByBatchIdAndTypeAndStatus(
                user.getBatch().getId(), PostType.EVENT, ApprovalStatus.APPROVED
        );
        List<Post> schoolEvents = postRepository.findBySchoolIdAndBatchIsNullAndTypeAndStatus(
                user.getSchool().getId(), PostType.EVENT, ApprovalStatus.APPROVED
        );
        List<Announcement> schoolAnnouncements =
                announcementRepository.findBySchoolIdAndBatchIsNullOrderByCreatedAtDesc(user.getSchool().getId());

        model.addAttribute("batchEvents", batchEvents);
        model.addAttribute("schoolEvents", schoolEvents);
        model.addAttribute("announcements", schoolAnnouncements);
        return "member/events";
    }

    @PostMapping("/event/{postId}/rsvp")
    public String rsvp(@PathVariable Long postId,
                       @RequestParam int guests,
                       @RequestParam(required = false) String note,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirect) {
        User user = getCurrentUser(userDetails);
        ensureMemberHasSchoolAndBatch(user);
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Event পোস্ট পাওয়া যায়নি"));
        ensureEventAccessible(user, post);
        if (post.getType() != PostType.EVENT || post.getStatus() != ApprovalStatus.APPROVED) {
            throw new RuntimeException("এই পোস্টে RSVP করা যাবে না");
        }

        RSVP existing = rsvpRepository.findByPostIdAndUserId(postId, user.getId()).orElse(null);
        if (existing == null) {
            RSVP rsvp = new RSVP();
            rsvp.setPost(post);
            rsvp.setUser(user);
            rsvp.setGuests(Math.max(1, guests));
            rsvp.setNote(note);
            rsvpRepository.save(rsvp);
        } else {
            existing.setGuests(Math.max(1, guests));
            existing.setNote(note);
            rsvpRepository.save(existing);
        }

        redirect.addFlashAttribute("message", "আরএসভিপি নিশ্চিত হয়েছে");
        return "redirect:/member/events";
    }

    @GetMapping("/notifications")
    public String notifications(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Notification> notifications = notificationService.getForRecipient(user.getId());
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", notificationService.unreadCount(user.getId()));
        return "member/notifications";
    }

    @PostMapping("/notifications/read/{id}")
    public String markNotificationRead(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        notificationService.markAsRead(id, user.getId());
        return "redirect:/member/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markNotificationsReadAll(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        notificationService.markAllRead(user.getId());
        return "redirect:/member/notifications";
    }

    @PostMapping("/notifications/mark-all-read")
    public String markNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        notificationService.markAllRead(user.getId());
        return "redirect:/member/notifications";
    }
}
