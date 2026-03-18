package org.hartford.fireinsurance.dto;

public class PremiumBreakdownLineItem {
    private String label;
    private String description;
    private Double amount;
    private String kind;

    public PremiumBreakdownLineItem() {
    }

    public PremiumBreakdownLineItem(String label, String description, Double amount, String kind) {
        this.label = label;
        this.description = description;
        this.amount = amount;
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}