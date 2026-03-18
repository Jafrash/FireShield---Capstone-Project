package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.model.Surveyor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimInspectionRepository extends JpaRepository<ClaimInspection,Long> {
    Optional<ClaimInspection> findByClaim(Claim claim);
    List<ClaimInspection> findBySurveyor(Surveyor surveyor);
    
    Optional<ClaimInspection> findTopByClaimAndStatusOrderByInspectionDateDesc(
            Claim claim, ClaimInspection.ClaimInspectionStatus status);

    @Query("SELECT i FROM ClaimInspection i WHERE i.claim.claimId = :claimId ORDER BY i.inspectionDate DESC")
    List<ClaimInspection> findCompletedByClaimId(@Param("claimId") Long claimId);
}
