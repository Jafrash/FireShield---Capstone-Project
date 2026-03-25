package org.hartford.fireinsurance.domain.event;

/**
 * Domain event fired when a new claim is submitted to the system.
 * This event triggers fraud analysis and initial processing workflows.
 */
public class ClaimSubmittedEvent extends DomainEvent {

    private final String customerName;
    private final Double claimAmount;
    private final String claimType;
    private final String submittedBy;

    public ClaimSubmittedEvent(Long claimId, String customerName, Double claimAmount,
                              String claimType, String submittedBy) {
        super(claimId);
        this.customerName = customerName;
        this.claimAmount = claimAmount;
        this.claimType = claimType;
        this.submittedBy = submittedBy;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Double getClaimAmount() {
        return claimAmount;
    }

    public String getClaimType() {
        return claimType;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public Long getClaimId() {
        return getAggregateId();
    }

    @Override
    public String getEventDetails() {
        return String.format("New claim submitted: ID=%d, Amount=%.2f, Type=%s, Customer=%s, Submitted by=%s",
            getClaimId(), claimAmount, claimType, customerName, submittedBy);
    }
}