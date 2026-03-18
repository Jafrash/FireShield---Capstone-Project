package org.hartford.fireinsurance.dto;
import org.hartford.fireinsurance.model.Claim;
public class UpdateClaimStatusRequest {
    private Claim.ClaimStatus status;
    public UpdateClaimStatusRequest() {}
    public Claim.ClaimStatus getStatus() { return status; }
    public void setStatus(Claim.ClaimStatus status) { this.status = status; }
}
