package com.alumniconnect.service;

import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final UserRepository userRepository;

    public MemberService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getPendingMembersBySchool(Long schoolId) {
        return userRepository.findBySchoolIdAndEnabledFalseAndRole(schoolId, Role.MEMBER);
    }

    public void approveMember(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setEnabled(true);
        userRepository.save(user);
    }

    public void rejectMember(Long memberId) {
        userRepository.deleteById(memberId);
    }

    public void verifyMember(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void assignBatchController(Long memberId, Long batchId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if (user.getBatch() == null || !batchId.equals(user.getBatch().getId())) {
            throw new IllegalArgumentException("সদস্য নির্বাচিত ব্যাচের অন্তর্ভুক্ত নয়");
        }
        user.setRole(Role.BATCH_CONTROLLER);
        userRepository.save(user);
    }

    public void assignBatchController(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setRole(Role.BATCH_CONTROLLER);
        userRepository.save(user);
    }

    public void removeBatchController(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setRole(Role.MEMBER);
        userRepository.save(user);
    }

    public void suspendMember(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setSuspended(true);
        userRepository.save(user);
    }

    public void unsuspendMember(Long memberId) {
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        user.setSuspended(false);
        userRepository.save(user);
    }

    public void deleteMember(Long memberId) {
        userRepository.deleteById(memberId);
    }

    public User editProfile(User existing, User updatedUser) {
        BeanUtils.copyProperties(
                updatedUser,
                existing,
                "id", "email", "password", "role", "enabled", "verified", "school", "batch", "createdAt"
        );
        return userRepository.save(existing);
    }
}
