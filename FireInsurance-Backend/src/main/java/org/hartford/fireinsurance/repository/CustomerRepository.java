package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUserUsername(String username);

}
