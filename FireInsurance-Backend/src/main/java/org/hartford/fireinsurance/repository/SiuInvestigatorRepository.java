package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.SiuInvestigator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiuInvestigatorRepository extends JpaRepository<SiuInvestigator, Long> {
    Optional<SiuInvestigator> findByUserUsername(String username);
}