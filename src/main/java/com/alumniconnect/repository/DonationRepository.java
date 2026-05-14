package com.alumniconnect.repository;

import com.alumniconnect.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByCampaignId(Long campaignId);

    List<Donation> findByDonorId(Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d WHERE d.donor.id = :userId")
    BigDecimal getTotalDonationsByDonorId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Donation d")
    BigDecimal getTotalDonations();
}
