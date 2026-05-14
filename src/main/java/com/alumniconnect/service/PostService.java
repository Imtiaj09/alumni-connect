package com.alumniconnect.service;

import com.alumniconnect.entity.ApprovalStatus;
import com.alumniconnect.entity.Post;
import com.alumniconnect.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> getPendingPostsBySchool(Long schoolId) {
        return postRepository.findBySchoolIdAndStatus(schoolId, ApprovalStatus.PENDING);
    }

    public List<Post> getPendingPostsByBatch(Long batchId) {
        return postRepository.findByBatchIdAndStatus(batchId, ApprovalStatus.PENDING);
    }

    public Post approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setStatus(ApprovalStatus.APPROVED);
        return postRepository.save(post);
    }

    public Post rejectPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setStatus(ApprovalStatus.REJECTED);
        return postRepository.save(post);
    }
}
