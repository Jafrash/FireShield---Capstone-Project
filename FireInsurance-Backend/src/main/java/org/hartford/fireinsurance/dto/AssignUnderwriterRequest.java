package org.hartford.fireinsurance.dto;

import jakarta.validation.constraints.NotNull;

public class AssignUnderwriterRequest {

    @NotNull
    private Long underwriterId;

    @NotNull
    private Long targetId;

    public Long getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(Long underwriterId) {
        this.underwriterId = underwriterId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}

