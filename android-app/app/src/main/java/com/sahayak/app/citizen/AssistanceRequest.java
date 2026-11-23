package com.sahayak.app.citizen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AssistanceRequest implements Serializable {
    public String requestId;
    public String citizenId;
    public String citizenName;
    public String beneficiaryName;
    public int age;
    public String gender;              // Male, Female, Other
    public String contactNumber;      // optional
    public String familyDetails;      // optional background
    public String houseAddress;
    public String locality;
    public String city;
    public String district;
    public String state;
    public String pincode;
    public Double latitude;           // optional map location
    public Double longitude;
    public String assistanceType;     // Medical, Groceries, Education, Financial, Elderly, Disability, Emergency
    public String description;
    public String reporterRelationship;
    public boolean consentConfirmed;
    public String status;             // see RequestStatus
    public String rejectionReason;    // optional
    public String assignedSanghaId;
    public String assignedSanghaName;
    public List<String> evidenceDocuments = new ArrayList<>();
    public long createdAt;            // millis
    public long updatedAt;
    public double targetAmount;       // For sponsorship
    public double raisedAmount;

    public AssistanceRequest() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.evidenceDocuments = new ArrayList<>();
    }

    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (houseAddress != null && !houseAddress.isEmpty()) sb.append(houseAddress).append(", ");
        if (locality != null && !locality.isEmpty()) sb.append(locality).append(", ");
        if (city != null && !city.isEmpty()) sb.append(city).append(", ");
        if (district != null && !district.isEmpty()) sb.append(district).append(" ");
        if (pincode != null && !pincode.isEmpty()) sb.append("- ").append(pincode);
        return sb.toString().trim();
    }
}