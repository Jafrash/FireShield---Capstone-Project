package org.hartford.fireinsurance.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "policy_subscriptions")
public class PolicySubscription {

    // Nested ENUM for Subscription Status
    public enum SubscriptionStatus {
        SUBMITTED,          // Customer submitted proposal
        PENDING,            // Customer applied for policy, pending admin action
        UNDER_REVIEW,       // Policy under review by underwriter
        INSPECTION_PENDING, // Inspection pending
        APPROVED,           // Policy approved by underwriter
        PAYMENT_PENDING,    // Awaiting payment confirmation
        REJECTED,           // Policy rejected by underwriter
        ACTIVE,             // Policy active
        // Legacy statuses retained for backward compatibility with existing data/workflows
        REQUESTED,          // Customer applied for policy
        INSPECTING,         // Surveyor inspecting property
        INSPECTED,          // Inspection completed, waiting admin approval
        EXPIRED,            // Policy term ended
        CANCELLED           // Policy cancelled
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "premium_amount")
    private Double premiumAmount;

    // NEW: Store base premium from policy (before risk adjustment)
    @Column(name = "base_premium_amount")
    private Double basePremiumAmount;

    // NEW: Store risk score from inspection
    @Column(name = "risk_score")
    private Double riskScore;

    // NEW: Store risk multiplier applied
    @Column(name = "risk_multiplier")
    private Double riskMultiplier;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prop_inspection_id")
    private Inspection propertyInspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "underwriter_id")
    private Underwriter underwriter;

    // RENEWAL WORKFLOW FIELDS
    @Column(name = "renewal_eligible")
    private Boolean renewalEligible = false;

    @Column(name = "previous_subscription_id")
    private Long previousSubscriptionId;

    @Column(name = "renewal_count")
    private Integer renewalCount = 0;

    @Column(name = "renewal_reminder_sent")
    private Boolean renewalReminderSent = false;

    // NO CLAIM BONUS (NCB) FIELDS
    @Column(name = "claim_free_years")
    private Integer claimFreeYears = 0;

    @Column(name = "ncb_discount")
    private Double ncbDiscount = 0.0;

    @Column(name = "last_claim_date")
    private LocalDateTime lastClaimDate;

    // COVERAGE AMOUNT REQUESTED BY CUSTOMER
    @Column(name = "requested_coverage")
    private Double requestedCoverage;

    // PROPOSAL / UNDERWRITING FIELDS (additive)
    @Column(name = "construction_type")
    private String constructionType;

    @Column(name = "roof_type")
    private String roofType;

    @Column(name = "number_of_floors")
    private Integer numberOfFloors;

    @Column(name = "occupancy_type")
    private String occupancyType;

    @Column(name = "manufacturing_process", columnDefinition = "TEXT")
    private String manufacturingProcess;

    @Column(name = "hazardous_goods", columnDefinition = "TEXT")
    private String hazardousGoods;

    @Column(name = "previous_loss_history", columnDefinition = "TEXT")
    private String previousLossHistory;

    @Column(name = "insurance_declined_before")
    private Boolean insuranceDeclinedBefore;

    @Column(name = "property_value")
    private Double propertyValue;

    @Column(name = "declaration_accepted")
    private Boolean declarationAccepted = false;

    @Column(name = "payment_received")
    private Boolean paymentReceived = false;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "cover_note_file_name")
    private String coverNoteFileName;

    @Column(name = "policy_document_file_name")
    private String policyDocumentFileName;

    @JsonManagedReference
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Claim> claims;

    public PolicySubscription() {
    }

    public PolicySubscription(Long subscriptionId, Customer customer, Policy policy, Property property,
                              LocalDate startDate, LocalDate endDate, SubscriptionStatus status, 
                              Double premiumAmount, List<Claim> claims) {
        this.subscriptionId = subscriptionId;
        this.customer = customer;
        this.policy = policy;
        this.property = property;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.premiumAmount = premiumAmount;
        this.claims = claims;
    }

    // Getters and Setters

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public Double getPremiumAmount() {
        return premiumAmount;
    }

    public void setPremiumAmount(Double premiumAmount) {
        this.premiumAmount = premiumAmount;
    }

    public Double getBasePremiumAmount() {
        return basePremiumAmount;
    }

    public void setBasePremiumAmount(Double basePremiumAmount) {
        this.basePremiumAmount = basePremiumAmount;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Double getRiskMultiplier() {
        return riskMultiplier;
    }

    public void setRiskMultiplier(Double riskMultiplier) {
        this.riskMultiplier = riskMultiplier;
    }

    public Inspection getPropertyInspection() {
        return propertyInspection;
    }

    public void setPropertyInspection(Inspection propertyInspection) {
        this.propertyInspection = propertyInspection;
    }

    public Underwriter getUnderwriter() {
        return underwriter;
    }

    public void setUnderwriter(Underwriter underwriter) {
        this.underwriter = underwriter;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    // Utility methods

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    public void activate() {
        this.setStatus(SubscriptionStatus.ACTIVE);
    }

    public void cancel() {
        this.setStatus(SubscriptionStatus.CANCELLED);
    }

    // RENEWAL WORKFLOW GETTERS AND SETTERS

    public Boolean getRenewalEligible() {
        return renewalEligible;
    }

    public void setRenewalEligible(Boolean renewalEligible) {
        this.renewalEligible = renewalEligible;
    }

    public Long getPreviousSubscriptionId() {
        return previousSubscriptionId;
    }

    public void setPreviousSubscriptionId(Long previousSubscriptionId) {
        this.previousSubscriptionId = previousSubscriptionId;
    }

    public Integer getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(Integer renewalCount) {
        this.renewalCount = renewalCount;
    }

    public Boolean getRenewalReminderSent() {
        return renewalReminderSent;
    }

    public void setRenewalReminderSent(Boolean renewalReminderSent) {
        this.renewalReminderSent = renewalReminderSent;
    }

    // NCB GETTERS AND SETTERS

    public Integer getClaimFreeYears() {
        return claimFreeYears;
    }

    public void setClaimFreeYears(Integer claimFreeYears) {
        this.claimFreeYears = claimFreeYears;
    }

    public Double getNcbDiscount() {
        return ncbDiscount;
    }

    public void setNcbDiscount(Double ncbDiscount) {
        this.ncbDiscount = ncbDiscount;
    }

    public LocalDateTime getLastClaimDate() {
        return lastClaimDate;
    }

    public void setLastClaimDate(LocalDateTime lastClaimDate) {
        this.lastClaimDate = lastClaimDate;
    }

    // UTILITY METHODS FOR RENEWAL

    public boolean isRenewalEligible() {
        if (this.endDate == null || this.status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        // Policy is eligible for renewal 30 days before expiry
        LocalDate today = LocalDate.now();
        LocalDate renewalStartDate = this.endDate.minusDays(30);
        return !today.isBefore(renewalStartDate);
    }

    public long getDaysRemaining() {
        if (this.endDate == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(today, this.endDate);
    }

    public Double getRequestedCoverage() {
        return requestedCoverage;
    }

    public void setRequestedCoverage(Double requestedCoverage) {
        this.requestedCoverage = requestedCoverage;
    }

    public String getConstructionType() {
        return constructionType;
    }

    public void setConstructionType(String constructionType) {
        this.constructionType = constructionType;
    }

    public String getRoofType() {
        return roofType;
    }

    public void setRoofType(String roofType) {
        this.roofType = roofType;
    }

    public Integer getNumberOfFloors() {
        return numberOfFloors;
    }

    public void setNumberOfFloors(Integer numberOfFloors) {
        this.numberOfFloors = numberOfFloors;
    }

    public String getOccupancyType() {
        return occupancyType;
    }

    public void setOccupancyType(String occupancyType) {
        this.occupancyType = occupancyType;
    }

    public String getManufacturingProcess() {
        return manufacturingProcess;
    }

    public void setManufacturingProcess(String manufacturingProcess) {
        this.manufacturingProcess = manufacturingProcess;
    }

    public String getHazardousGoods() {
        return hazardousGoods;
    }

    public void setHazardousGoods(String hazardousGoods) {
        this.hazardousGoods = hazardousGoods;
    }

    public String getPreviousLossHistory() {
        return previousLossHistory;
    }

    public void setPreviousLossHistory(String previousLossHistory) {
        this.previousLossHistory = previousLossHistory;
    }

    public Boolean getInsuranceDeclinedBefore() {
        return insuranceDeclinedBefore;
    }

    public void setInsuranceDeclinedBefore(Boolean insuranceDeclinedBefore) {
        this.insuranceDeclinedBefore = insuranceDeclinedBefore;
    }

    public Double getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(Double propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Boolean getDeclarationAccepted() { return declarationAccepted; }
    public void setDeclarationAccepted(Boolean declarationAccepted) { this.declarationAccepted = declarationAccepted; }

    public Boolean getPaymentReceived() {
        return paymentReceived;
    }

    public void setPaymentReceived(Boolean paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getCoverNoteFileName() {
        return coverNoteFileName;
    }

    public void setCoverNoteFileName(String coverNoteFileName) {
        this.coverNoteFileName = coverNoteFileName;
    }

    public String getPolicyDocumentFileName() {
        return policyDocumentFileName;
    }

    public void setPolicyDocumentFileName(String policyDocumentFileName) {
        this.policyDocumentFileName = policyDocumentFileName;
    }
}
