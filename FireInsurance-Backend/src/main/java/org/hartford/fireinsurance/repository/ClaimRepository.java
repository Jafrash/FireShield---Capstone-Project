package org.hartford.fireinsurance.repository;


import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Underwriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim,Long> {

    List<Claim> findBySubscription(

            PolicySubscription subscription);

    // Find all claims for a specific customer
    @Query("SELECT c FROM Claim c WHERE c.subscription.customer = :customer")
    List<Claim> findByCustomer(@Param("customer") Customer customer);

    List<Claim> findByUnderwriter(Underwriter underwriter);
    List<Claim> findByUnderwriterAndStatus(Underwriter underwriter, Claim.ClaimStatus status);
}
