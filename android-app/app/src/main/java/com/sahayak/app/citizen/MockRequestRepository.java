package com.sahayak.app.citizen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MockRequestRepository {
    private static final List<AssistanceRequest> requests = new ArrayList<>();
    private static int counter = 100;

    static {
        // Sample request 1: Education Fees Support (Under Verification)
        AssistanceRequest r1 = new AssistanceRequest();
        r1.requestId = "REQ-2026-0001";
        r1.citizenId = "usr-cit-001";
        r1.citizenName = "Ramesh Kumar";
        r1.beneficiaryName = "Suresh Bhat";
        r1.age = 16;
        r1.gender = "Male";
        r1.contactNumber = "9876012345";
        r1.familyDetails = "Single mother household. Mother works as household helper.";
        r1.houseAddress = "Door No. 4-12, Near Kadri Temple";
        r1.locality = "Kadri";
        r1.city = "Mangaluru";
        r1.district = "Dakshina Kannada";
        r1.state = "Karnataka";
        r1.pincode = "575002";
        r1.latitude = 12.8756;
        r1.longitude = 74.8432;
        r1.assistanceType = "Education Fees";
        r1.description = "10th Grade student requiring assistance for high school fees and textbook purchases for the upcoming academic year.";
        r1.reporterRelationship = "Neighbour";
        r1.consentConfirmed = true;
        r1.status = RequestStatus.UNDER_VERIFICATION;
        r1.assignedSanghaId = "SNG-001";
        r1.assignedSanghaName = "Seva Bharathi Community Welfare Trust";
        r1.evidenceDocuments = Arrays.asList("Marks_Card_Grade9.pdf", "Fee_Structure_Receipt.jpg", "Income_Certificate_Copy.jpg");
        r1.targetAmount = 15000;
        r1.raisedAmount = 5000;
        r1.createdAt = System.currentTimeMillis() - 86400000L * 2;
        r1.updatedAt = System.currentTimeMillis() - 86400000L;
        requests.add(r1);

        // Sample request 2: Medical Treatment Support (Verified & Needing Funds)
        AssistanceRequest r2 = new AssistanceRequest();
        r2.requestId = "REQ-2026-0002";
        r2.citizenId = "usr-cit-001";
        r2.citizenName = "Ramesh Kumar";
        r2.beneficiaryName = "Sharada Amma";
        r2.age = 68;
        r2.gender = "Female";
        r2.contactNumber = "9845112233";
        r2.familyDetails = "Elderly widow living alone with no family income support.";
        r2.houseAddress = "Flat 2B, Kodialbail Colony";
        r2.locality = "Kodialbail";
        r2.city = "Mangaluru";
        r2.district = "Dakshina Kannada";
        r2.state = "Karnataka";
        r2.pincode = "575003";
        r2.latitude = 12.8701;
        r2.longitude = 74.8380;
        r2.assistanceType = "Medical Treatment";
        r2.description = "Requires financial aid for urgent cataract surgery and ongoing cardiac medication.";
        r2.reporterRelationship = "Social Worker";
        r2.consentConfirmed = true;
        r2.status = RequestStatus.VERIFIED;
        r2.assignedSanghaId = "SNG-002";
        r2.assignedSanghaName = "Karuna Social Support & Care Sangha";
        r2.evidenceDocuments = Arrays.asList("Hospital_Diagnosis_Report.pdf", "Prescription_Card.jpg");
        r2.targetAmount = 25000;
        r2.raisedAmount = 18000;
        r2.createdAt = System.currentTimeMillis() - 86400000L * 5;
        r2.updatedAt = System.currentTimeMillis() - 86400000L * 3;
        requests.add(r2);

        // Sample request 3: Groceries and Food (Completed)
        AssistanceRequest r3 = new AssistanceRequest();
        r3.requestId = "REQ-2026-0003";
        r3.citizenId = "usr-cit-001";
        r3.citizenName = "Ramesh Kumar";
        r3.beneficiaryName = "Ganesh & Family";
        r3.age = 42;
        r3.gender = "Male";
        r3.contactNumber = "9741098765";
        r3.familyDetails = "Daily wage worker family disabled due to accident, 3 dependents.";
        r3.houseAddress = "House No. 89, Surathkal Beach Road";
        r3.locality = "Surathkal";
        r3.city = "Mangaluru";
        r3.district = "Dakshina Kannada";
        r3.state = "Karnataka";
        r3.pincode = "575014";
        r3.latitude = 12.9812;
        r3.longitude = 74.8021;
        r3.assistanceType = "Groceries and Food";
        r3.description = "Monthly ration kit support required following breadwinner's physical injury.";
        r3.reporterRelationship = "Community Volunteer";
        r3.consentConfirmed = true;
        r3.status = RequestStatus.COMPLETED;
        r3.assignedSanghaId = "SNG-003";
        r3.assignedSanghaName = "Asha Deepa Educational & Welfare Foundation";
        r3.evidenceDocuments = Arrays.asList("Ration_Card_Copy.pdf", "Disability_Medical_Certificate.jpg");
        r3.targetAmount = 5000;
        r3.raisedAmount = 5000;
        r3.createdAt = System.currentTimeMillis() - 86400000L * 10;
        r3.updatedAt = System.currentTimeMillis() - 86400000L * 2;
        requests.add(r3);

        counter = 3;
    }

    public static synchronized AssistanceRequest submit(AssistanceRequest r) {
        counter++;
        r.requestId = String.format(Locale.US, "REQ-2026-%04d", counter);
        r.status = RequestStatus.SUBMITTED;
        r.createdAt = System.currentTimeMillis();
        r.updatedAt = System.currentTimeMillis();

        // Auto assign local Sangha based on locality/default
        if (r.assignedSanghaName == null || r.assignedSanghaName.isEmpty()) {
            r.assignedSanghaId = "SNG-001";
            r.assignedSanghaName = "Seva Bharathi Community Welfare Trust";
            r.status = RequestStatus.ASSIGNED;
        }

        requests.add(0, r);
        return r;
    }

    public static synchronized List<AssistanceRequest> getMine(String citizenId) {
        if (citizenId == null || citizenId.isEmpty()) return new ArrayList<>(requests);
        List<AssistanceRequest> list = new ArrayList<>();
        for (AssistanceRequest r : requests) {
            if (citizenId.equalsIgnoreCase(r.citizenId) || r.citizenId == null) {
                list.add(r);
            }
        }
        return list;
    }

    public static synchronized List<AssistanceRequest> getAllRequests() {
        return new ArrayList<>(requests);
    }

    public static synchronized List<AssistanceRequest> getVerifiedRequestsNeedingSponsorship() {
        List<AssistanceRequest> list = new ArrayList<>();
        for (AssistanceRequest r : requests) {
            if ((RequestStatus.VERIFIED.equals(r.status) || RequestStatus.IN_PROGRESS.equals(r.status))
                    && r.targetAmount > r.raisedAmount) {
                list.add(r);
            }
        }
        return list;
    }

    public static synchronized AssistanceRequest getById(String requestId) {
        if (requestId == null) return null;
        for (AssistanceRequest r : requests) {
            if (requestId.equalsIgnoreCase(r.requestId)) return r;
        }
        return null;
    }

    public static synchronized void updateStatus(String requestId, String newStatus, String rejectionReason) {
        AssistanceRequest r = getById(requestId);
        if (r != null) {
            r.status = newStatus;
            r.updatedAt = System.currentTimeMillis();
            if (rejectionReason != null) {
                r.rejectionReason = rejectionReason;
            }
        }
    }

    public static synchronized void addRaisedAmount(String requestId, double amount) {
        AssistanceRequest r = getById(requestId);
        if (r != null) {
            r.raisedAmount += amount;
            r.updatedAt = System.currentTimeMillis();
            if (r.raisedAmount >= r.targetAmount && r.targetAmount > 0) {
                r.status = RequestStatus.IN_PROGRESS;
            }
        }
    }
}