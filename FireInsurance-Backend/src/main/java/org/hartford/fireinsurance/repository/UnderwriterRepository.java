package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Underwriter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnderwriterRepository extends JpaRepository<Underwriter, Long> {
    Optional<Underwriter> findByUserUsername(String username);
}

