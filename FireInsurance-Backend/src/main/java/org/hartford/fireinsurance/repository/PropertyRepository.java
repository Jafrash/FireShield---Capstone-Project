package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property,Long> {
    List<Property> findByCustomer(Customer customer);

    // Fraud Detection: Check for duplicate addresses across different customers
    @Query("SELECT COUNT(p) FROM Property p WHERE LOWER(p.address) = LOWER(:address) AND p.customer.customerId != :excludeCustomerId")
    long countDuplicateAddresses(@Param("address") String address, @Param("excludeCustomerId") Long excludeCustomerId);
}
