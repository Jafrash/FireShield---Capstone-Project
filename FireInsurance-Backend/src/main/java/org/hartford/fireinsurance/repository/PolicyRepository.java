package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy,Long> {
}
