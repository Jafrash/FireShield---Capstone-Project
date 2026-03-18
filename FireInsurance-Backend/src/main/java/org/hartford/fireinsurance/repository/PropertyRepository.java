package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property,Long> {
    List<Property> findByCustomer(Customer customer);
}
