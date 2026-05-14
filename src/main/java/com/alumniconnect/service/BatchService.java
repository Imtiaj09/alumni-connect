package com.alumniconnect.service;

import com.alumniconnect.entity.Role;
import com.alumniconnect.entity.User;
import com.alumniconnect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class BatchService {

    private final UserRepository userRepository;

    public BatchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getMembersByBatch(Long batchId) {
        return userRepository.findByBatchIdAndRoleIn(
                batchId,
                Arrays.asList(Role.MEMBER, Role.BATCH_CONTROLLER)
        );
    }
}
