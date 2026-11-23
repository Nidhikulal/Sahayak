package com.sahayak.app.models;

import java.io.Serializable;

public class Donation implements Serializable {
    public static final String TYPE_GENERAL = "GENERAL_DONATION";
    public static final String TYPE_REQUEST_SPECIFIC = "REQUEST_SPECIFIC";

    private String donationId;
    private String citizenId;
    private String donorName;
    private String sanghaId;
    private String sanghaName;
    private String contributionType; // GENERAL_DONATION or REQUEST_SPECIFIC
    private String requestId;        // optional
    private double amount;
    private String purpose;
    private String transactionRef;
    private String paymentMethod;
    private long transactionDate;
    private String status;           // SUCCESS

    public Donation() {
        this.transactionDate = System.currentTimeMillis();
        this.status = "SUCCESS";
    }

    public Donation(String donationId, String citizenId, String donorName, String sanghaId, String sanghaName, String contributionType, String requestId, double amount, String purpose, String transactionRef, String paymentMethod) {
        this.donationId = donationId;
        this.citizenId = citizenId;
        this.donorName = donorName;
        this.sanghaId = sanghaId;
        this.sanghaName = sanghaName;
        this.contributionType = contributionType;
        this.requestId = requestId;
        this.amount = amount;
        this.purpose = purpose;
        this.transactionRef = transactionRef;
        this.paymentMethod = paymentMethod;
        this.transactionDate = System.currentTimeMillis();
        this.status = "SUCCESS";
    }

    public String getDonationId() {
        return donationId;
    }

    public void setDonationId(String donationId) {
        this.donationId = donationId;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getSanghaId() {
        return sanghaId;
    }

    public void setSanghaId(String sanghaId) {
        this.sanghaId = sanghaId;
    }

    public String getSanghaName() {
        return sanghaName;
    }

    public void setSanghaName(String sanghaName) {
        this.sanghaName = sanghaName;
    }

    public String getContributionType() {
        return contributionType;
    }

    public void setContributionType(String contributionType) {
        this.contributionType = contributionType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(long transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}