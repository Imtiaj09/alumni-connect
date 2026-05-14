package com.alumniconnect.repository;

import com.alumniconnect.entity.RSVP;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RSVPRepository extends JpaRepository<RSVP, Long> {
    Optional<RSVP> findByPostIdAndUserId(Long postId, Long userId);

    List<RSVP> findByPostId(Long postId);
}

