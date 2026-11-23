package com.sahayak.app.models;

import java.io.Serializable;

public class User implements Serializable {
    public static final String ROLE_CITIZEN = "CITIZEN";
    public static final String ROLE_SANGHA = "SANGHA";

    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    private String userId;
    private String fullName;
    private String email;
    private String mobile;
    private String password;
    private String address;
    private String role; // CITIZEN or SANGHA
    private String approvalStatus; // APPROVED, PENDING, REJECTED, SUSPENDED
    private String registrationNo; // For Sangha
    private String description; // For Sangha
    private long registeredAt;

    public User() {
    }

    public User(String userId, String fullName, String email, String mobile, String password, String address, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.address = address;
        this.role = role;
        this.approvalStatus = ROLE_CITIZEN.equals(role) ? STATUS_APPROVED : STATUS_PENDING;
        this.registeredAt = System.currentTimeMillis();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(long registeredAt) {
        this.registeredAt = registeredAt;
    }
}