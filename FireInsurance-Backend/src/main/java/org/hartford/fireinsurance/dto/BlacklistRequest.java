package org.hartford.fireinsurance.dto;

import org.hartford.fireinsurance.model.Blacklist.BlacklistType;

/**
 * Request DTO for adding entries to the blacklist.
 */
public class BlacklistRequest {

    private BlacklistType type;
    private String value;
    private String reason;

    // Constructors
    public BlacklistRequest() {
    }

    public BlacklistRequest(BlacklistType type, String value, String reason) {
        this.type = type;
        this.value = value;
        this.reason = reason;
    }

    // Getters and Setters
    public BlacklistType getType() {
        return type;
    }

    public void setType(BlacklistType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
