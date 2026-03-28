package org.hartford.fireinsurance.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO for SIU investigation action responses.
 * Used to return success/failure status and updated claim information.
 */
public class SiuInvestigationActionResponse {

    private boolean success;
    private String message;
    private String claimId;
    private String newStatus;
    private String timestamp;

    // Constructors
    public SiuInvestigationActionResponse() {}

    public SiuInvestigationActionResponse(boolean success, String message, String claimId, String newStatus) {
        this.success = success;
        this.message = message;
        this.claimId = claimId;
        this.newStatus = newStatus;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Static factory methods for common responses
    public static SiuInvestigationActionResponse success(String claimId, String newStatus, String message) {
        return new SiuInvestigationActionResponse(true, message, claimId, newStatus);
    }

    public static SiuInvestigationActionResponse failure(String claimId, String message) {
        return new SiuInvestigationActionResponse(false, message, claimId, null);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "SiuInvestigationActionResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", claimId='" + claimId + '\'' +
                ", newStatus='" + newStatus + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}