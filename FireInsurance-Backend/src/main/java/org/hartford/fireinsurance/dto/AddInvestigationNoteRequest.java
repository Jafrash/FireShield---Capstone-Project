package org.hartford.fireinsurance.dto;

/**
 * Request DTO for adding investigation notes to a case.
 */
public class AddInvestigationNoteRequest {

    private String note;

    // Constructors
    public AddInvestigationNoteRequest() {
    }

    public AddInvestigationNoteRequest(String note) {
        this.note = note;
    }

    // Getters and Setters
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}