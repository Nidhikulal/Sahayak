package com.sahayak.app.models;

import java.io.Serializable;

public class SanghaContribution implements Serializable {
    public static final String TYPE_RECEIVED = "RECEIVED";
    public static final String TYPE_GIVEN = "GIVEN";

    private String contributionId;
    private String sanghaId;
    private String otherSanghaName;
    private String direction; // RECEIVED or GIVEN
    private double amount;
    private String purpose;
    private String serviceCategory;
    private String transactionRef;
    private long date;

    public SanghaContribution() {
        this.date = System.currentTimeMillis();
    }

    public SanghaContribution(String contributionId, String sanghaId, String otherSanghaName, String direction, double amount, String purpose, String serviceCategory, String transactionRef) {
        this.contributionId = contributionId;
        this.sanghaId = sanghaId;
        this.otherSanghaName = otherSanghaName;
        this.direction = direction;
        this.amount = amount;
        this.purpose = purpose;
        this.serviceCategory = serviceCategory;
        this.transactionRef = transactionRef;
        this.date = System.currentTimeMillis();
    }

    public String getContributionId() {
        return contributionId;
    }

    public void setContributionId(String contributionId) {
        this.contributionId = contributionId;
    }

    public String getSanghaId() {
        return sanghaId;
    }

    public void setSanghaId(String sanghaId) {
        this.sanghaId = sanghaId;
    }

    public String getOtherSanghaName() {
        return otherSanghaName;
    }

    public void setOtherSanghaName(String otherSanghaName) {
        this.otherSanghaName = otherSanghaName;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
