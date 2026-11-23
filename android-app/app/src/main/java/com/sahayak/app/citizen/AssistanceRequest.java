package com.sahayak.app.citizen;

import java.io.Serializable;

public class AssistanceRequest implements Serializable {
    public String requestId;
    public String beneficiaryName;
    public String contactNumber;      // optional
    public String houseAddress;
    public String locality;
    public String district;
    public Double latitude;           // filled by Member 4's map picker later
    public Double longitude;
    public String assistanceType;     // FOOD | EDUCATION | MEDICAL | OTHER
    public String description;
    public String reporterRelationship;
    public boolean consentConfirmed;
    public String status;             // see RequestStatus
    public String rejectionReason;    // optional
    public long createdAt;            // millis; API will send ISO string later
}