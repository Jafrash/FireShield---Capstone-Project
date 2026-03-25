package org.hartford.fireinsurance.repository;

import org.hartford.fireinsurance.model.InvestigationNote;
import org.hartford.fireinsurance.model.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing InvestigationNote entities.
 */
@Repository
public interface InvestigationNoteRepository extends JpaRepository<InvestigationNote, Long> {

    // Find notes by investigation case
    List<InvestigationNote> findByInvestigationCase(InvestigationCase investigationCase);

    // Find notes by investigation case ordered by creation date
    List<InvestigationNote> findByInvestigationCaseOrderByCreatedAtDesc(InvestigationCase investigationCase);

    // Find notes by investigation case and type
    List<InvestigationNote> findByInvestigationCaseAndNoteType(InvestigationCase investigationCase,
                                                              InvestigationNote.NoteType noteType);

    // Find important notes
    List<InvestigationNote> findByInvestigationCaseAndIsImportantTrue(InvestigationCase investigationCase);

    // Find notes by creator
    List<InvestigationNote> findByCreatedBy(String createdBy);

    // Search notes by content
    @Query("SELECT n FROM InvestigationNote n WHERE n.investigationCase = :investigationCase " +
           "AND (LOWER(n.noteContent) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(n.noteTitle) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<InvestigationNote> findByInvestigationCaseAndContentContaining(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("searchTerm") String searchTerm);

    // Find recent notes
    @Query("SELECT n FROM InvestigationNote n WHERE n.investigationCase = :investigationCase " +
           "AND n.createdAt >= :sinceDate ORDER BY n.createdAt DESC")
    List<InvestigationNote> findRecentNotesByCase(
            @Param("investigationCase") InvestigationCase investigationCase,
            @Param("sinceDate") LocalDateTime sinceDate);

    // Count notes by investigation case
    long countByInvestigationCase(InvestigationCase investigationCase);
}