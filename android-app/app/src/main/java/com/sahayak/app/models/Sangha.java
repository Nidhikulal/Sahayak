package com.sahayak.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Sangha implements Serializable {
    private String sanghaId;
    private String name;
    private String registrationNo;
    private String organizationType = "Trust / NGO";
    private String establishedYear = "2015";
    private String locality;
    private String city;
    private String district;
    private String fullAddress;
    private String shortDescription;
    private String fullDescription;
    private String contactPhone;
    private String contactEmail;
    private String presidentName = "Dr. Ramesh Bhat";
    private String secretaryName = "Suresh Poojary";
    private String approvalStatus; // APPROVED, PENDING, REJECTED, SUSPENDED
    private List<String> assistanceActivities;
    private double rating;
    private int totalBeneficiariesHelped;

    public Sangha() {
        this.assistanceActivities = new ArrayList<>();
    }

    public Sangha(String sanghaId, String name, String registrationNo, String locality, String city, String district, String fullAddress, String shortDescription, String fullDescription, String contactPhone, String contactEmail, String approvalStatus) {
        this.sanghaId = sanghaId;
        this.name = name;
        this.registrationNo = registrationNo;
        this.locality = locality;
        this.city = city;
        this.district = district;
        this.fullAddress = fullAddress;
        this.shortDescription = shortDescription;
        this.fullDescription = fullDescription;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.approvalStatus = approvalStatus;
        this.assistanceActivities = new ArrayList<>();
        this.rating = 4.8;
        this.totalBeneficiariesHelped = 120;
    }

    public String getSanghaId() {
        return sanghaId;
    }

    public void setSanghaId(String sanghaId) {
        this.sanghaId = sanghaId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getEstablishedYear() {
        return establishedYear;
    }

    public void setEstablishedYear(String establishedYear) {
        this.establishedYear = establishedYear;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getFullDescription() {
        return fullDescription;
    }

    public void setFullDescription(String fullDescription) {
        this.fullDescription = fullDescription;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getPresidentName() {
        return presidentName;
    }

    public void setPresidentName(String presidentName) {
        this.presidentName = presidentName;
    }

    public String getSecretaryName() {
        return secretaryName;
    }

    public void setSecretaryName(String secretaryName) {
        this.secretaryName = secretaryName;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public List<String> getAssistanceActivities() {
        return assistanceActivities;
    }

    public void setAssistanceActivities(List<String> assistanceActivities) {
        this.assistanceActivities = assistanceActivities;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getTotalBeneficiariesHelped() {
        return totalBeneficiariesHelped;
    }

    public void setTotalBeneficiariesHelped(int totalBeneficiariesHelped) {
        this.totalBeneficiariesHelped = totalBeneficiariesHelped;
    }
}
