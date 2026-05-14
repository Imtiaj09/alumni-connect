package com.alumniconnect.repository;

import com.alumniconnect.entity.FundCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundCampaignRepository extends JpaRepository<FundCampaign, Long> {
    List<FundCampaign> findBySchoolIdAndClosedFalse(Long schoolId);

    List<FundCampaign> findByBatchIdAndClosedFalse(Long batchId);

    long countBySchoolIdAndClosedFalse(Long schoolId);
}
