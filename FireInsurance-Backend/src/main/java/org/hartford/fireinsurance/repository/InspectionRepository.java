package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection,Long> {
    List<Inspection> findByProperty(Property property);

    List<Inspection> findBySurveyor(Surveyor surveyor);

    List<Inspection> findBySubscription(PolicySubscription subscription);

    java.util.Optional<Inspection> findTopByPropertyAndStatusOrderByInspectionDateDesc(
            Property property, Inspection.InspectionStatus status);

    @Query("SELECT i FROM Inspection i WHERE i.property.propertyId = :propertyId AND i.status = 'COMPLETED' ORDER BY i.inspectionDate DESC")
    List<Inspection> findCompletedByPropertyId(@Param("propertyId") Long propertyId);

    @Query("SELECT i FROM Inspection i WHERE LOWER(TRIM(i.property.address)) = LOWER(TRIM(:address)) AND i.status = 'COMPLETED'")
    List<Inspection> findCompletedByAddress(@Param("address") String address);
}