package org.hartford.fireinsurance.model;

/**
 * Enum defining specialization areas for SIU investigators
 */
public enum InvestigatorSpecialization {
    FIRE("Fire-related fraud investigations"),
    THEFT("Theft and burglary investigations"),
    FRAUD("General fraud and suspicious activity"),
    PROPERTY("Property damage investigations"),
    GENERAL("General investigations across all areas");

    private final String description;

    InvestigatorSpecialization(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}