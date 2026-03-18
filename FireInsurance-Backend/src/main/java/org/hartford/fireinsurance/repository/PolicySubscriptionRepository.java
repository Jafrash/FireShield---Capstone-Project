package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.PolicySubscription.SubscriptionStatus;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Underwriter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicySubscriptionRepository extends JpaRepository<PolicySubscription,Long> {
    List<PolicySubscription> findByCustomer(Customer customer);

    List<PolicySubscription> findByProperty(Property property);

    PolicySubscription findByPropertyInspection(org.hartford.fireinsurance.model.Inspection propertyInspection);
    
    List<PolicySubscription> findByStatus(SubscriptionStatus status);

    List<PolicySubscription> findByUnderwriter(Underwriter underwriter);

    List<PolicySubscription> findByUnderwriterAndStatus(Underwriter underwriter, SubscriptionStatus status);
}
