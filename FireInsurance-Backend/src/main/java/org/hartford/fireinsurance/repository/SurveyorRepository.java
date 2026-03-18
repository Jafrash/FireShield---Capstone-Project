package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyorRepository extends JpaRepository<Surveyor,Long> {
    Optional<Surveyor> findByUser(User user);
    Optional<Surveyor> findByUserUsername(String username);
    boolean existsByLicenseNumber(String licenseNumber);
}
