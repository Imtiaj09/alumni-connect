package com.alumniconnect.service;

import com.alumniconnect.entity.Donation;
import com.alumniconnect.entity.FundCampaign;
import com.alumniconnect.repository.DonationRepository;
import com.alumniconnect.repository.FundCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FundCampaignService {

    private final FundCampaignRepository campaignRepository;
    private final DonationRepository donationRepository;

    public FundCampaignService(FundCampaignRepository campaignRepository,
                               DonationRepository donationRepository) {
        this.campaignRepository = campaignRepository;
        this.donationRepository = donationRepository;
    }

    public FundCampaign create(FundCampaign campaign) {
        if (campaign.getCollectedAmount() == null) {
            campaign.setCollectedAmount(BigDecimal.ZERO);
        }
        return campaignRepository.save(campaign);
    }

    public FundCampaign findById(Long id) {
        return campaignRepository.findById(id).orElseThrow(() -> new RuntimeException("Campaign not found"));
    }

    @Transactional
    public Donation donate(Donation donation, FundCampaign campaign) {
        donation.setCampaign(campaign);
        Donation savedDonation = donationRepository.save(donation);
        BigDecimal collected = campaign.getCollectedAmount() == null ? BigDecimal.ZERO : campaign.getCollectedAmount();
        BigDecimal amount = donation.getAmount() == null ? BigDecimal.ZERO : donation.getAmount();
        campaign.setCollectedAmount(collected.add(amount));
        campaignRepository.save(campaign);
        return savedDonation;
    }

    public void closeCampaign(Long id) {
        FundCampaign campaign = findById(id);
        campaign.setClosed(true);
        campaignRepository.save(campaign);
    }

    public List<Donation> getDonations(Long campaignId) {
        return donationRepository.findByCampaignId(campaignId);
    }
}
