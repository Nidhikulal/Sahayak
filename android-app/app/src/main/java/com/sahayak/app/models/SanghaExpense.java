package com.sahayak.app.models;

import java.io.Serializable;

public class SanghaExpense implements Serializable {
    private String expenseId;
    private String sanghaId;
    private double amount;
    private String purpose;
    private String serviceCategory;
    private String requestId;
    private String transactionRef;
    private long expenseDate;

    public SanghaExpense() {
        this.expenseDate = System.currentTimeMillis();
    }

    public SanghaExpense(String expenseId, String sanghaId, double amount, String purpose, String serviceCategory, String requestId, String transactionRef) {
        this.expenseId = expenseId;
        this.sanghaId = sanghaId;
        this.amount = amount;
        this.purpose = purpose;
        this.serviceCategory = serviceCategory;
        this.requestId = requestId;
        this.transactionRef = transactionRef;
        this.expenseDate = System.currentTimeMillis();
    }

    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getSanghaId() {
        return sanghaId;
    }

    public void setSanghaId(String sanghaId) {
        this.sanghaId = sanghaId;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public long getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(long expenseDate) {
        this.expenseDate = expenseDate;
    }
}
