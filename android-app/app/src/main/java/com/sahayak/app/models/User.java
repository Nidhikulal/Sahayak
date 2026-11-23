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
    private String dateOfBirth;
    private String gender;
    private String localityCity;
    private String pinCode;
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

    public User(String userId, String fullName, String email, String mobile, String password, String dateOfBirth, String gender, String localityCity, String pinCode, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.localityCity = localityCity;
        this.address = localityCity; // Set address to locality and city
        this.pinCode = pinCode;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLocalityCity() {
        return localityCity;
    }

    public void setLocalityCity(String localityCity) {
        this.localityCity = localityCity;
        if (this.address == null || this.address.isEmpty()) {
            this.address = localityCity;
        }
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
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