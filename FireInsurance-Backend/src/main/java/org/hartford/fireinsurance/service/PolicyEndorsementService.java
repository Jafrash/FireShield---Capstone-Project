package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CreateEndorsementRequest;
import org.hartford.fireinsurance.model.PolicyEndorsement;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.repository.PolicyEndorsementRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PolicyEndorsementService {

    private final PolicyEndorsementRepository endorsementRepository;
    private final PolicySubscriptionRepository subscriptionRepository;

    public PolicyEndorsementService(PolicyEndorsementRepository endorsementRepository,
                                    PolicySubscriptionRepository subscriptionRepository) {
        this.endorsementRepository = endorsementRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public PolicyEndorsement create(String username, CreateEndorsementRequest request) {
        PolicySubscription subscription = subscriptionRepository.findById(request.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + request.getSubscriptionId()));

        if (!subscription.getCustomer().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: subscription does not belong to this customer");
        }

        PolicyEndorsement endorsement = new PolicyEndorsement();
        endorsement.setSubscription(subscription);
        endorsement.setChangeType(request.getChangeType());
        endorsement.setRequestedCoverage(request.getRequestedCoverage());
        endorsement.setNewOccupancyType(request.getNewOccupancyType());
        endorsement.setNewHazardousGoods(request.getNewHazardousGoods());
        endorsement.setReason(request.getReason());
        endorsement.setStatus(PolicyEndorsement.EndorsementStatus.REQUESTED);
        endorsement.setRequestedBy(username);
        endorsement.setCreatedAt(LocalDateTime.now());

        return endorsementRepository.save(endorsement);
    }

    public PolicyEndorsement review(Long endorsementId, boolean approve, String reviewer) {
        PolicyEndorsement endorsement = endorsementRepository.findById(endorsementId)
                .orElseThrow(() -> new RuntimeException("Endorsement not found with ID: " + endorsementId));

        if (endorsement.getStatus() != PolicyEndorsement.EndorsementStatus.REQUESTED) {
            throw new RuntimeException("Only REQUESTED endorsements can be reviewed");
        }

        if (approve) {
            PolicySubscription subscription = endorsement.getSubscription();
            if (endorsement.getRequestedCoverage() != null && endorsement.getRequestedCoverage() > 0) {
                Double maxCoverage = subscription.getPolicy() != null ? subscription.getPolicy().getMaxCoverageAmount() : null;
                subscription.setRequestedCoverage(maxCoverage != null
                        ? Math.min(endorsement.getRequestedCoverage(), maxCoverage)
                        : endorsement.getRequestedCoverage());
            }
            if (endorsement.getNewOccupancyType() != null && !endorsement.getNewOccupancyType().isBlank()) {
                subscription.setOccupancyType(endorsement.getNewOccupancyType());
            }
            if (endorsement.getNewHazardousGoods() != null) {
                subscription.setHazardousGoods(endorsement.getNewHazardousGoods());
            }
            subscriptionRepository.save(subscription);
            endorsement.setStatus(PolicyEndorsement.EndorsementStatus.APPROVED);
        } else {
            endorsement.setStatus(PolicyEndorsement.EndorsementStatus.REJECTED);
        }

        endorsement.setReviewedBy(reviewer);
        endorsement.setReviewedAt(LocalDateTime.now());
        return endorsementRepository.save(endorsement);
    }

    public List<PolicyEndorsement> getBySubscription(Long subscriptionId, String username) {
        PolicySubscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with ID: " + subscriptionId));

        if (!subscription.getCustomer().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized: subscription does not belong to this customer");
        }

        return endorsementRepository.findBySubscription(subscription);
    }

    public List<PolicyEndorsement> getAll() {
        return endorsementRepository.findAll();
    }
}
