package org.hartford.fireinsurance.dto;

import java.util.Set;

/**
 * Request DTO for updating evidence metadata.
 */
public class EvidenceUpdateRequest {

    private String description;
    private Set<String> tags;
    private Boolean isCritical;

    // Constructors
    public EvidenceUpdateRequest() {
    }

    public EvidenceUpdateRequest(String description, Set<String> tags, Boolean isCritical) {
        this.description = description;
        this.tags = tags;
        this.isCritical = isCritical;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Boolean getIsCritical() {
        return isCritical;
    }

    public void setIsCritical(Boolean isCritical) {
        this.isCritical = isCritical;
    }
}