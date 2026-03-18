package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Document;
import org.hartford.fireinsurance.model.Document.DocumentStage;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Surveyor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document,Long> {
    List<Document> findByClaim(Claim claim);
    List<Document> findByProperty(Property property);
    List<Document> findByCustomer(Customer customer);
    List<Document> findBySurveyor(Surveyor surveyor);
    List<Document> findByDocumentStage(DocumentStage stage);
    List<Document> findByClaimAndDocumentStage(Claim claim, DocumentStage stage);
    List<Document> findByPropertyAndDocumentStage(Property property, DocumentStage stage);
}
