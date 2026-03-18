package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.PolicyEndorsement;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyEndorsementRepository extends JpaRepository<PolicyEndorsement, Long> {
    List<PolicyEndorsement> findBySubscription(PolicySubscription subscription);
}
